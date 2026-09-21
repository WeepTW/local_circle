package com.example.localcircle.presentation;

import com.example.localcircle.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ProblemAdvice {
  private static final Logger log = LoggerFactory.getLogger(ProblemAdvice.class);

  private ResponseEntity<ApiProblem> problem(
      int status, String code, String detail, HttpServletRequest request) {
    var p = ApiProblem.of(status, code, detail, request.getRequestURI());
    if (status >= 500) log.error("request_failed code={} traceId={}", code, p.traceId());
    return ResponseEntity.status(status).header("Content-Type", "application/problem+json").body(p);
  }

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ApiProblem> domain(DomainException e, HttpServletRequest r) {
    return problem(e.status(), e.code(), e.getMessage(), r);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiProblem> invalid(Exception e, HttpServletRequest r) {
    return problem(400, "INVALID_REQUEST", "Check required fields and allowed numeric ranges.", r);
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  ResponseEntity<ApiProblem> concurrent(Exception e, HttpServletRequest r) {
    return problem(409, "CONCURRENCY_CONFLICT", "Concurrent change; reload before retrying.", r);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiProblem> unexpected(Exception e, HttpServletRequest r) {
    return problem(500, "INTERNAL_ERROR", "Unable to complete request.", r);
  }
}
