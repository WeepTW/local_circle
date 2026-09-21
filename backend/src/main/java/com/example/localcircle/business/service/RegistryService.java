package com.example.localcircle.business.service;

import com.example.localcircle.business.calculator.DoubleFeeCalculator;
import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.policy.ProductAuthorizationPolicy;
import com.example.localcircle.business.port.RegistryRepository;
import com.example.localcircle.common.error.DomainException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistryService {
  private final RegistryRepository repository;

  public RegistryService(RegistryRepository repository) {
    this.repository = repository;
  }

  public Actor actor(long id) {
    return repository
        .actor(id)
        .orElseThrow(
            () ->
                new DomainException(401, "UNAUTHENTICATED", "Select a valid local demo identity."));
  }

  public List<Product> products() {
    return repository.products(false);
  }

  public List<Product> adminProducts(Actor a) {
    if (!"ADMIN".equals(a.role())) throw DomainException.forbidden();
    return repository.products(true);
  }

  public List<Account> accounts(Actor a) {
    return repository.accounts(a.userId());
  }

  public List<Preference> preferences(Actor a) {
    return repository.preferences(a.userId());
  }

  public Preference preference(Actor a, long id) {
    return repository.preference(id, a.userId()).orElseThrow(DomainException::missing);
  }

  private Product checkedProduct(Actor a, SaveCommand c) {
    if (c.productId() < 1 || c.accountId() < 1) throw DomainException.invalid();
    var product =
        repository
            .product(c.productId(), true)
            .filter(Product::active)
            .orElseThrow(DomainException::missing);
    repository.account(c.accountId(), a.userId()).orElseThrow(DomainException::missing);
    return product;
  }

  @Transactional
  public Preference save(Actor a, SaveCommand c) {
    var p = checkedProduct(a, c);
    var amounts = DoubleFeeCalculator.calculate(p.price(), p.feeRate(), c.plannedQuantity());
    long id = repository.insert(a.userId(), c, p, amounts);
    repository.audit(a.userId(), id, "SAVE");
    return preference(a, id);
  }

  @Transactional
  public Preference update(Actor a, long id, long version, SaveCommand c) {
    if (version < 0) throw DomainException.invalid();
    preference(a, id);
    var p = checkedProduct(a, c);
    var amounts = DoubleFeeCalculator.calculate(p.price(), p.feeRate(), c.plannedQuantity());
    if (repository.update(id, a.userId(), version, c, p, amounts) != 1)
      throw DomainException.conflict();
    repository.audit(a.userId(), id, "UPDATE");
    return preference(a, id);
  }

  @Transactional
  public void delete(Actor a, long id, long version) {
    if (version < 0) throw DomainException.invalid();
    preference(a, id);
    if (repository.delete(id, a.userId(), version) != 1) throw DomainException.conflict();
    repository.audit(a.userId(), id, "DELETE");
  }

  @Transactional
  public Product updateProduct(Actor a, long id, ProductCommand c) {
    var p = repository.product(id, false).orElseThrow(DomainException::missing);
    ProductAuthorizationPolicy.requireEdit(a, p);
    if (c.productName() == null
        || c.productName().isBlank()
        || c.productName().length() > 160
        || c.version() < 0) throw DomainException.invalid();
    DoubleFeeCalculator.calculate(c.price(), c.feeRate(), 1);
    if (repository.updateProduct(id, a.userId(), c) != 1) throw DomainException.conflict();
    return repository.product(id, false).orElseThrow(DomainException::missing);
  }
}
