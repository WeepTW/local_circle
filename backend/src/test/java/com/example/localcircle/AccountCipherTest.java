package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;

import com.example.localcircle.common.security.AccountCipher;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountCipherTest {
  @TempDir Path directory;

  private AccountCipher cipher(String name, String key) throws Exception {
    Path file = directory.resolve(name);
    Files.writeString(file, "id=test-key\nkey=" + key);
    return new AccountCipher(file.toString());
  }

  @Test
  void authenticatedRandomizedEncryptionPreservesLeadingZeros() throws Exception {
    var cipher = cipher("key", "01".repeat(32));
    var first = cipher.encrypt(1, "account-a", "001234567890");
    var second = cipher.encrypt(1, "account-a", "001234567890");
    assertNotEquals(first.payload(), second.payload());
    assertEquals("001234567890", cipher.decrypt(1, "account-a", first.keyId(), first.payload()));
    assertThrows(
        RuntimeException.class,
        () -> cipher.decrypt(2, "account-a", first.keyId(), first.payload()));
    assertThrows(
        RuntimeException.class,
        () -> cipher.decrypt(1, "account-b", first.keyId(), first.payload()));
    assertThrows(
        RuntimeException.class, () -> cipher.decrypt(1, "account-a", "unknown", first.payload()));
    byte[] tampered = java.util.Base64.getDecoder().decode(first.payload());
    tampered[tampered.length - 1] ^= 1;
    String modified = java.util.Base64.getEncoder().encodeToString(tampered);
    assertThrows(RuntimeException.class, () -> cipher.decrypt(1, "account-a", first.keyId(), modified));
    var wrong = cipher("wrong", "02".repeat(32));
    assertThrows(
        RuntimeException.class,
        () -> wrong.decrypt(1, "account-a", first.keyId(), first.payload()));
    assertThrows(
        RuntimeException.class, () -> cipher.decrypt(1, "account-a", first.keyId(), "AAAA"));
  }

  @Test
  void missingOrInvalidKeyFailsWithoutReplacingFile() throws Exception {
    assertThrows(
        IllegalStateException.class,
        () -> new AccountCipher(directory.resolve("missing").toString()));
    assertThrows(IllegalStateException.class, () -> cipher("invalid", "abcd"));
    assertEquals("id=test-key\nkey=abcd", Files.readString(directory.resolve("invalid")));
  }
}
