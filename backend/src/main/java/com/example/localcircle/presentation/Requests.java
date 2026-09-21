package com.example.localcircle.presentation;

import jakarta.validation.constraints.*;

public final class Requests {
  private Requests() {}

  public record Save(
      @NotNull @Positive Long productId,
      @NotNull @Positive Long accountId,
      @NotNull @Min(1) @Max(1000000) Integer plannedQuantity) {}

  public record Update(
      @NotNull @Positive Long productId,
      @NotNull @Positive Long accountId,
      @NotNull @Min(1) @Max(1000000) Integer plannedQuantity,
      @NotNull @PositiveOrZero Long version) {}

  public record ProductUpdate(
      @NotBlank @Size(max = 160) String productName,
      @NotNull @DecimalMin("0") @DecimalMax("999999999999") Double price,
      @NotNull @DecimalMin("0") @DecimalMax("1") Double feeRate,
      @NotNull Boolean active,
      @NotNull @PositiveOrZero Long version) {}
}
