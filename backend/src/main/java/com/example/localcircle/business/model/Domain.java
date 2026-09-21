package com.example.localcircle.business.model;

import java.time.Instant;
import java.util.Set;

public final class Domain {
  private Domain() {}

  public record Actor(long userId, String userName, String email, String role, Set<String> labels) {
    public Actor {
      labels = Set.copyOf(labels);
    }
  }

  public record Product(
      long productId,
      String productCode,
      String productName,
      double price,
      double feeRate,
      String currency,
      String ownerLabel,
      boolean active,
      long version) {}

  public record Account(long accountId, String maskedAccount, String currency, long version) {}

  public record Preference(
      long preferenceId,
      long productId,
      String productCode,
      String productName,
      long accountId,
      String maskedAccount,
      String userEmail,
      int plannedQuantity,
      double priceSnapshot,
      double feeRateSnapshot,
      double baseAmount,
      double totalFee,
      double totalAmount,
      long version,
      Instant savedAt,
      Instant updatedAt) {}

  public record SaveCommand(long productId, long accountId, int plannedQuantity) {}

  public record ProductCommand(
      String productName, double price, double feeRate, boolean active, long version) {}

  public record Amounts(double baseAmount, double totalFee, double totalAmount) {}
}
