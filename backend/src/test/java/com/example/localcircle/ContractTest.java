package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;

import com.example.localcircle.business.calculator.DoubleFeeCalculator;
import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.policy.ProductAuthorizationPolicy;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContractTest {
  @Test
  void arithmeticHasNoBusinessRounding() {
    var a = DoubleFeeCalculator.calculate(60, .001, 5);
    assertEquals(300.3, a.totalAmount());
    assertEquals(.1 * 3 * .001, DoubleFeeCalculator.calculate(.1, .001, 3).totalFee());
    assertEquals(0, DoubleFeeCalculator.calculate(1, 0, 2).totalFee());
  }

  @Test
  void rejectsInvalidAndNonFinite() {
    for (double n : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertThrows(RuntimeException.class, () -> DoubleFeeCalculator.calculate(n, .1, 1));
      assertThrows(RuntimeException.class, () -> DoubleFeeCalculator.calculate(1, n, 1));
    }
    assertThrows(RuntimeException.class, () -> DoubleFeeCalculator.calculate(1, .1, 0));
    assertThrows(RuntimeException.class, () -> DoubleFeeCalculator.calculate(1, 2, 1));
    assertThrows(
        RuntimeException.class, () -> DoubleFeeCalculator.calculate(999999999999.0, 1, 1000000));
  }

  @Test
  void adminAndLabelAreBothRequired() {
    var p = new Product(1, "0050", "DEMO", 60, .001, "TWD", "ETF", true, 0);
    assertFalse(
        ProductAuthorizationPolicy.canEdit(
            new Actor(1, "a", "a@example.test", "USER", Set.of("ETF")), p));
    assertFalse(
        ProductAuthorizationPolicy.canEdit(
            new Actor(1, "a", "a@example.test", "ADMIN", Set.of("OTHER")), p));
    assertTrue(
        ProductAuthorizationPolicy.canEdit(
            new Actor(1, "a", "a@example.test", "ADMIN", Set.of("ETF")), p));
  }
}
