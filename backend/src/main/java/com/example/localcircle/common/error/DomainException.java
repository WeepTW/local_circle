package com.example.localcircle.common.error;

public class DomainException extends RuntimeException {
  private final int status;
  private final String code;

  public DomainException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static DomainException invalid() {
    return new DomainException(400, "INVALID_INPUT", "Invalid input or numeric range.");
  }

  public static DomainException missing() {
    return new DomainException(404, "NOT_FOUND", "Resource not found.");
  }

  public static DomainException forbidden() {
    return new DomainException(403, "FORBIDDEN", "This operation is not permitted.");
  }

  public static DomainException conflict() {
    return new DomainException(
        409, "VERSION_CONFLICT", "Resource changed; reload before retrying.");
  }
}
