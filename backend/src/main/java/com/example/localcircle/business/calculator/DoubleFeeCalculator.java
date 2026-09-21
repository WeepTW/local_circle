package com.example.localcircle.business.calculator;

import com.example.localcircle.business.model.Domain.Amounts;
import com.example.localcircle.common.error.DomainException;

public final class DoubleFeeCalculator {
  private DoubleFeeCalculator() {}

  public static Amounts calculate(double price, double rate, int quantity) {
    if (!Double.isFinite(price)
        || !Double.isFinite(rate)
        || price < 0
        || price > 999999999999.0
        || rate < 0
        || rate > 1
        || quantity < 1
        || quantity > 1000000) throw DomainException.invalid();
    double base = price * quantity, fee = base * rate, total = base + fee;
    if (!Double.isFinite(total) || total >= 1e16) throw DomainException.invalid();
    return new Amounts(base, fee, total);
  }
}
