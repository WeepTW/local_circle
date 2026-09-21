package com.example.localcircle.business.policy;

import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.common.error.DomainException;

public final class ProductAuthorizationPolicy {
  private ProductAuthorizationPolicy() {}

  public static boolean canEdit(Actor actor, Product product) {
    return "ADMIN".equals(actor.role()) && actor.labels().contains(product.ownerLabel());
  }

  public static void requireEdit(Actor actor, Product product) {
    if (!canEdit(actor, product)) throw DomainException.forbidden();
  }
}
