package com.example.localcircle.common.error;

import java.util.UUID;

public record ApiProblem(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String code,
    String traceId) {
  public static ApiProblem of(int status, String code, String detail, String path) {
    return new ApiProblem(
        "urn:local-circle:problem:" + code.toLowerCase(java.util.Locale.ROOT),
        code,
        status,
        detail,
        path,
        code,
        UUID.randomUUID().toString());
  }
}
