package com.example.localcircle.business.service;

import com.example.localcircle.business.model.Domain.Actor;
import com.example.localcircle.business.model.Domain.Product;
import com.example.localcircle.business.model.PersonalPreference.*;
import com.example.localcircle.business.port.*;
import com.example.localcircle.common.error.DomainException;
import com.example.localcircle.common.security.AccountCipher;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalPreferenceService {
  private final RegistryRepository registry;
  private final PersonalPreferenceRepository repository;
  private final AccountCipher cipher;

  public PersonalPreferenceService(
      RegistryRepository registry, PersonalPreferenceRepository repository, AccountCipher cipher) {
    this.registry = registry;
    this.repository = repository;
    this.cipher = cipher;
  }

  public List<View> list(Actor actor) {
    return repository.list(actor.userId());
  }

  public View get(Actor actor, long id) {
    return repository.get(actor.userId(), id).orElseThrow(DomainException::missing);
  }

  public String accountNumber(Actor actor, long id) {
    var account = repository.account(actor.userId(), id).orElseThrow(DomainException::missing);
    if (account.payload() == null)
      throw new DomainException(
          409,
          "ACCOUNT_NUMBER_REQUIRED",
          "Enter a complete account number; the legacy record contains only its last four digits.");
    return cipher.decrypt(actor.userId(), account.reference(), account.keyId(), account.payload());
  }

  private Values values(Actor actor, Input input) {
    if (input.plannedQuantity() == null
        || input.plannedQuantity() < 1
        || input.plannedQuantity() > 1000000
        || (input.accountId() == null) == (input.accountNumber() == null))
      throw DomainException.invalid();
    Product source = null;
    if (input.productId() != null) {
      if (input.productId() < 1) throw DomainException.invalid();
      source =
          registry
              .product(input.productId(), true)
              .filter(Product::active)
              .orElseThrow(DomainException::missing);
    }
    boolean custom =
        input.productName() != null || input.price() != null || input.feeRate() != null;
    if (!custom && source == null) throw DomainException.invalid();
    String name = custom ? input.productName() : source.productName();
    BigDecimal price = custom ? input.price() : BigDecimal.valueOf(source.price());
    BigDecimal rate = custom ? input.feeRate() : BigDecimal.valueOf(source.feeRate());
    if (name == null
        || name.isBlank()
        || name.length() > 160
        || price == null
        || rate == null
        || price.signum() < 0
        || price.compareTo(new BigDecimal("999999999999")) > 0
        || rate.signum() < 0
        || rate.compareTo(BigDecimal.ONE) > 0
        || price.stripTrailingZeros().scale() > 8
        || rate.stripTrailingZeros().scale() > 12) throw DomainException.invalid();
    BigDecimal base =
        price
            .multiply(BigDecimal.valueOf(input.plannedQuantity()))
            .setScale(8, RoundingMode.HALF_UP);
    BigDecimal fee = base.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    BigDecimal total = base.add(fee);
    if (total.compareTo(new BigDecimal("10000000000000000")) >= 0) throw DomainException.invalid();
    long accountId;
    if (input.accountId() != null) {
      accountId = input.accountId();
      if (accountId < 1) throw DomainException.invalid();
      registry.account(accountId, actor.userId()).orElseThrow(DomainException::missing);
    } else {
      String number = input.accountNumber();
      if (!number.matches("[0-9]{6,32}")) throw DomainException.invalid();
      String reference = UUID.randomUUID().toString();
      var sealed = cipher.encrypt(actor.userId(), reference, number);
      accountId =
          repository.createAccount(
              actor.userId(),
              reference,
              number.substring(number.length() - 4),
              sealed.keyId(),
              sealed.payload());
    }
    return new Values(
        input.productId(),
        accountId,
        input.plannedQuantity(),
        name.strip(),
        price,
        rate,
        base,
        fee,
        total);
  }

  @Transactional
  public View save(Actor actor, Input input) {
    var values = values(actor, input);
    long id = repository.insert(actor.userId(), values);
    registry.audit(actor.userId(), id, "SAVE");
    return get(actor, id);
  }

  @Transactional
  public View update(Actor actor, long id, Input input) {
    if (input.version() == null || input.version() < 0) throw DomainException.invalid();
    get(actor, id);
    var values = values(actor, input);
    if (repository.update(actor.userId(), id, input.version(), values) != 1)
      throw DomainException.conflict();
    registry.audit(actor.userId(), id, "UPDATE");
    return get(actor, id);
  }

  @Transactional
  public void delete(Actor actor, long id, long version) {
    get(actor, id);
    if (registry.delete(id, actor.userId(), version) != 1) throw DomainException.conflict();
    registry.audit(actor.userId(), id, "DELETE");
  }
}
