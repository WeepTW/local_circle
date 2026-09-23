package com.example.localcircle.data;

import com.example.localcircle.common.security.AccountCipher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify the configured key before accepting local demo traffic; seed only fresh synthetic data.
 */
@Component
@Profile("local")
public class AccountKeyVerifier implements ApplicationRunner {
  private final ProcedureExecutor executor;
  private final AccountCipher cipher;

  public AccountKeyVerifier(ProcedureExecutor executor, AccountCipher cipher) {
    this.executor = executor;
    this.cipher = cipher;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    var state =
        executor
            .call(
                "{call sp_key_state_lock()}",
                (r, n) ->
                    new State(
                        r.getString("key_id"),
                        r.getString("key_check"),
                        r.getBoolean("seed_accounts")))
            .getFirst();
    if (state.check() != null) {
      if (!"000000000000".equals(cipher.decrypt(0, "key-check", state.id(), state.check())))
        throw new IllegalStateException("Invalid account key.");
    } else {
      var sealed = cipher.encrypt(0, "key-check", "000000000000");
      executor.call(
          "{call sp_key_state_set(?,?)}", (r, n) -> r.getInt(1), sealed.keyId(), sealed.payload());
    }
    if (state.seed()) {
      var accounts =
          executor.call(
              "{call sp_account_seed_list()}",
              (r, n) ->
                  new Seed(
                      r.getLong("account_id"),
                      r.getLong("user_id"),
                      r.getString("account_ref"),
                      r.getString("last4")));
      for (var account : accounts) {
        var sealed =
            cipher.encrypt(account.owner(), account.reference(), "000000" + account.last4());
        executor.call(
            "{call sp_account_seed_complete(?,?,?)}",
            (r, n) -> r.getInt(1),
            account.id(),
            sealed.keyId(),
            sealed.payload());
      }
      executor.call("{call sp_account_seed_finish()}", (r, n) -> r.getInt(1));
    }
  }

  private record State(String id, String check, boolean seed) {}

  private record Seed(long id, long owner, String reference, String last4) {}
}
