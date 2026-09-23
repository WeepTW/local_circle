package com.example.localcircle.common.security;

import com.example.localcircle.common.error.DomainException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Authenticated encryption; no account or key material is logged or included in errors. */
@Component
public final class AccountCipher {
  private final String keyId;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public record Sealed(String keyId, String payload) {}

  public AccountCipher(
      @Value("${account.key-file:${ACCOUNT_KEY_FILE:../.secrets/account.key}}") String file) {
    try (var input = Files.newInputStream(Path.of(file))) {
      var properties = new Properties();
      properties.load(input);
      keyId = properties.getProperty("id", "");
      byte[] bytes = HexFormat.of().parseHex(properties.getProperty("key", ""));
      if (!keyId.matches("[A-Za-z0-9_-]{1,64}") || bytes.length != 32)
        throw new IllegalArgumentException();
      key = new SecretKeySpec(bytes, "AES");
      Arrays.fill(bytes, (byte) 0);
    } catch (Exception error) {
      throw new IllegalStateException(
          "Account encryption key missing, unreadable or invalid; restore the configured key.");
    }
  }

  private Cipher cipher(int mode, long owner, String reference, byte[] nonce) throws Exception {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key, new GCMParameterSpec(128, nonce));
    cipher.updateAAD(
        ("local_circle:account:v1:" + owner + ":" + reference).getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  public Sealed encrypt(long owner, String reference, String accountNumber) {
    if (accountNumber == null || !accountNumber.matches("[0-9]{6,32}"))
      throw DomainException.invalid();
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      byte[] ciphertext =
          cipher(Cipher.ENCRYPT_MODE, owner, reference, nonce)
              .doFinal(accountNumber.getBytes(StandardCharsets.UTF_8));
      return new Sealed(
          keyId,
          Base64.getEncoder()
              .encodeToString(
                  ByteBuffer.allocate(nonce.length + ciphertext.length)
                      .put(nonce)
                      .put(ciphertext)
                      .array()));
    } catch (Exception error) {
      throw unavailable();
    }
  }

  public String decrypt(long owner, String reference, String storedKeyId, String payload) {
    try {
      if (!keyId.equals(storedKeyId)) throw new IllegalArgumentException();
      byte[] bytes = Base64.getDecoder().decode(payload);
      if (bytes.length < 34) throw new IllegalArgumentException();
      return new String(
          cipher(Cipher.DECRYPT_MODE, owner, reference, Arrays.copyOf(bytes, 12))
              .doFinal(Arrays.copyOfRange(bytes, 12, bytes.length)),
          StandardCharsets.UTF_8);
    } catch (Exception error) {
      throw unavailable();
    }
  }

  private DomainException unavailable() {
    return new DomainException(
        503,
        "ACCOUNT_KEY_UNAVAILABLE",
        "Account data cannot be decrypted with the configured key.");
  }
}
