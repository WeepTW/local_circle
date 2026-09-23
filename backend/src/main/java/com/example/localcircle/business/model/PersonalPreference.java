package com.example.localcircle.business.model;

import java.math.BigDecimal;
import java.time.Instant;

public final class PersonalPreference {
  private PersonalPreference() {}

  public record Input(
      Long productId,
      Long accountId,
      String accountNumber,
      Integer plannedQuantity,
      String productName,
      BigDecimal price,
      BigDecimal feeRate,
      Long version) {
    @Override
    public String toString() {
      return "PersonalPreference.Input[redacted]";
    }
  }

  public record View(
      long preferenceId,
      Long productId,
      String productCode,
      String productName,
      long accountId,
      String maskedAccount,
      boolean accountNumberAvailable,
      String userEmail,
      int plannedQuantity,
      BigDecimal priceSnapshot,
      BigDecimal feeRateSnapshot,
      BigDecimal baseAmount,
      BigDecimal totalFee,
      BigDecimal totalAmount,
      long version,
      Instant savedAt,
      Instant updatedAt) {}

  public record Values(
      Long productId,
      long accountId,
      int quantity,
      String name,
      BigDecimal price,
      BigDecimal rate,
      BigDecimal base,
      BigDecimal fee,
      BigDecimal total) {}

  public record EncryptedAccount(
      long id, long owner, String reference, String keyId, String payload) {
    @Override
    public String toString() {
      return "EncryptedAccount[redacted]";
    }
  }
}
