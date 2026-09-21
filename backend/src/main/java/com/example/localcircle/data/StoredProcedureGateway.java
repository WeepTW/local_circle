package com.example.localcircle.data;

import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.port.RegistryRepository;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** All application SQL is a fixed CALL; values are bound, never interpolated. */
@Repository
public class StoredProcedureGateway implements RegistryRepository {
  private final ProcedureExecutor executor;

  public StoredProcedureGateway(ProcedureExecutor executor) {
    this.executor = executor;
  }

  private static Product productRow(ResultSet r, int rowNumber) throws SQLException {
    return new Product(
        r.getLong("product_id"),
        r.getString("product_code"),
        r.getString("product_name"),
        r.getBigDecimal("price").doubleValue(),
        r.getBigDecimal("fee_rate").doubleValue(),
        r.getString("currency"),
        r.getString("owner_label"),
        r.getBoolean("active"),
        r.getLong("version"));
  }

  private static Account accountRow(ResultSet r, int rowNumber) throws SQLException {
    return new Account(
        r.getLong("account_id"),
        "******" + r.getString("last4"),
        r.getString("currency"),
        r.getLong("version"));
  }

  private static Preference preferenceRow(ResultSet r, int rowNumber) throws SQLException {
    return new Preference(
        r.getLong("preference_id"),
        r.getLong("product_id"),
        r.getString("product_code"),
        r.getString("product_name"),
        r.getLong("account_id"),
        "******" + r.getString("last4"),
        r.getString("email"),
        r.getInt("planned_quantity"),
        r.getBigDecimal("price_snapshot").doubleValue(),
        r.getBigDecimal("fee_rate_snapshot").doubleValue(),
        r.getBigDecimal("base_amount").doubleValue(),
        r.getBigDecimal("total_fee").doubleValue(),
        r.getBigDecimal("total_amount").doubleValue(),
        r.getLong("version"),
        r.getTimestamp("saved_at").toInstant(),
        r.getTimestamp("updated_at").toInstant());
  }

  @Override
  public Optional<Actor> actor(long uid) {
    var rows =
        executor.call(
            "{call sp_actor_get(?)}",
            (r, rowNumber) ->
                new Actor(
                    r.getLong("user_id"),
                    r.getString("user_name"),
                    r.getString("email"),
                    r.getString("role"),
                    Set.of()),
            uid);
    return rows.stream()
        .findFirst()
        .map(
            a ->
                new Actor(
                    a.userId(),
                    a.userName(),
                    a.email(),
                    a.role(),
                    new HashSet<>(
                        executor.call(
                            "{call sp_actor_labels(?)}", (r, rowNumber) -> r.getString(1), uid))));
  }

  @Override
  public List<Product> products(boolean all) {
    return executor.call(
        all ? "{call sp_product_list_all()}" : "{call sp_product_list_active()}",
        StoredProcedureGateway::productRow);
  }

  @Override
  public Optional<Product> product(long id, boolean lock) {
    return executor
        .call(
            lock ? "{call sp_product_lock(?)}" : "{call sp_product_get(?)}",
            StoredProcedureGateway::productRow,
            id)
        .stream()
        .findFirst();
  }

  @Override
  public List<Account> accounts(long uid) {
    return executor.call(
        "{call sp_account_list_by_user(?)}", StoredProcedureGateway::accountRow, uid);
  }

  @Override
  public Optional<Account> account(long id, long uid) {
    return executor
        .call("{call sp_account_lock(?,?)}", StoredProcedureGateway::accountRow, id, uid)
        .stream()
        .findFirst();
  }

  @Override
  public List<Preference> preferences(long uid) {
    return executor.call(
        "{call sp_preference_list(?)}", StoredProcedureGateway::preferenceRow, uid);
  }

  @Override
  public Optional<Preference> preference(long id, long uid) {
    return executor
        .call("{call sp_preference_get_owned(?,?)}", StoredProcedureGateway::preferenceRow, id, uid)
        .stream()
        .findFirst();
  }

  @Override
  public long insert(long uid, SaveCommand c, Product p, Amounts a) {
    return executor
        .call(
            "{call sp_preference_insert(?,?,?,?,?,?,?,?,?)}",
            (r, rowNumber) -> r.getLong(1),
            uid,
            c.productId(),
            c.accountId(),
            c.plannedQuantity(),
            p.price(),
            p.feeRate(),
            a.baseAmount(),
            a.totalFee(),
            a.totalAmount())
        .getFirst();
  }

  @Override
  public int update(long id, long uid, long version, SaveCommand c, Product p, Amounts a) {
    return executor
        .call(
            "{call sp_preference_update_owned(?,?,?,?,?,?,?,?,?,?,?)}",
            (r, rowNumber) -> r.getInt(1),
            id,
            uid,
            version,
            c.productId(),
            c.accountId(),
            c.plannedQuantity(),
            p.price(),
            p.feeRate(),
            a.baseAmount(),
            a.totalFee(),
            a.totalAmount())
        .getFirst();
  }

  @Override
  public int delete(long id, long uid, long version) {
    return executor
        .call(
            "{call sp_preference_delete_owned(?,?,?)}",
            (r, rowNumber) -> r.getInt(1),
            id,
            uid,
            version)
        .getFirst();
  }

  @Override
  public int updateProduct(long id, long uid, ProductCommand c) {
    return executor
        .call(
            "{call sp_product_update(?,?,?,?,?,?,?)}",
            (r, rowNumber) -> r.getInt(1),
            id,
            uid,
            c.version(),
            c.productName(),
            c.price(),
            c.feeRate(),
            c.active())
        .getFirst();
  }

  @Override
  public void audit(long uid, long id, String action) {
    executor.call("{call sp_audit_insert(?,?,?)}", (r, rowNumber) -> r.getLong(1), uid, id, action);
  }
}
