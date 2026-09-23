package com.example.localcircle.data;

import com.example.localcircle.business.model.PersonalPreference.*;
import com.example.localcircle.business.port.PersonalPreferenceRepository;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class PersonalPreferenceGateway implements PersonalPreferenceRepository {
  private final ProcedureExecutor executor;

  public PersonalPreferenceGateway(ProcedureExecutor executor) {
    this.executor = executor;
  }

  private static View row(ResultSet r, int n) throws SQLException {
    return new View(
        r.getLong("preference_id"),
        r.getObject("product_id", Long.class),
        r.getString("product_code"),
        r.getString("resolved_name"),
        r.getLong("account_id"),
        "******" + r.getString("last4"),
        r.getBoolean("number_available"),
        r.getString("email"),
        r.getInt("planned_quantity"),
        r.getBigDecimal("price_snapshot"),
        r.getBigDecimal("fee_rate_snapshot"),
        r.getBigDecimal("base_amount"),
        r.getBigDecimal("total_fee"),
        r.getBigDecimal("total_amount"),
        r.getLong("version"),
        r.getTimestamp("saved_at").toInstant(),
        r.getTimestamp("updated_at").toInstant());
  }

  public List<View> list(long owner) {
    return executor.call("{call sp_personal_list(?)}", PersonalPreferenceGateway::row, owner);
  }

  public Optional<View> get(long owner, long id) {
    return executor
        .call("{call sp_personal_get(?,?)}", PersonalPreferenceGateway::row, owner, id)
        .stream()
        .findFirst();
  }

  public long insert(long owner, Values v) {
    return executor
        .call(
            "{call sp_personal_insert(?,?,?,?,?,?,?,?,?,?)}",
            (r, n) -> r.getLong(1),
            owner,
            v.productId(),
            v.accountId(),
            v.quantity(),
            v.name(),
            v.price(),
            v.rate(),
            v.base(),
            v.fee(),
            v.total())
        .getFirst();
  }

  public int update(long owner, long id, long version, Values v) {
    return executor
        .call(
            "{call sp_personal_update(?,?,?,?,?,?,?,?,?,?,?,?)}",
            (r, n) -> r.getInt(1),
            owner,
            id,
            version,
            v.productId(),
            v.accountId(),
            v.quantity(),
            v.name(),
            v.price(),
            v.rate(),
            v.base(),
            v.fee(),
            v.total())
        .getFirst();
  }

  public long createAccount(
      long owner, String reference, String last4, String keyId, String payload) {
    return executor
        .call(
            "{call sp_account_create_encrypted(?,?,?,?,?)}",
            (r, n) -> r.getLong(1),
            owner,
            reference,
            last4,
            keyId,
            payload)
        .getFirst();
  }

  public Optional<EncryptedAccount> account(long owner, long id) {
    return executor
        .call(
            "{call sp_account_encrypted_owned(?,?)}",
            (r, n) ->
                new EncryptedAccount(
                    r.getLong("account_id"),
                    owner,
                    r.getString("account_ref"),
                    r.getString("key_id"),
                    r.getString("number_ciphertext")),
            owner,
            id)
        .stream()
        .findFirst();
  }
}
