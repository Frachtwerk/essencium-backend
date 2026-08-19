/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns anything no other advice handles into a {@link ProblemDetail}, so no error escapes as a
 * plain Spring error response. Runs last, which keeps the catch-all out of {@link
 * GlobalExceptionHandler} and lets Spring resolve wrapped exceptions to their specific handler.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class FallbackExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(FallbackExceptionHandler.class);

  private final ProblemDetailFactory problemDetailFactory;

  public FallbackExceptionHandler(ProblemDetailFactory problemDetailFactory) {
    this.problemDetailFactory = problemDetailFactory;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnhandledException(
      Exception exception, HttpServletRequest request) {
    ResponseStatus responseStatus = findResponseStatus(exception);

    if (responseStatus != null) {
      return declaredStatusResponse(exception, responseStatus, request);
    }

    log.error("Unhandled exception", exception);

    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_SERVER_ERROR,
        "Internal server error",
        exception,
        request);
  }

  /**
   * {@code ResponseStatusExceptionResolver} would read the annotation, but it never runs: the
   * handler above matches first. Without this the status an application declares on its exception
   * would silently become 500.
   */
  private ResponseEntity<ProblemDetail> declaredStatusResponse(
      Exception exception, ResponseStatus responseStatus, HttpServletRequest request) {
    HttpStatus status = responseStatus.code();
    String reason = responseStatus.reason();
    String detail = reason.isBlank() ? exception.getMessage() : reason;

    if (status.is5xxServerError()) {
      log.error("Exception declaring {}", status, exception);
    } else {
      log.debug("Request rejected with declared {}: {}", status, exception.toString());
    }

    return response(status, ErrorCode.forStatus(status), detail, exception, request);
  }

  private ResponseEntity<ProblemDetail> response(
      HttpStatusCode status,
      ProblemErrorCode errorCode,
      @Nullable String detail,
      Exception exception,
      HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(problemDetailFactory.create(status, errorCode, detail, exception, request));
  }

  private static @Nullable ResponseStatus findResponseStatus(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      ResponseStatus responseStatus =
          AnnotatedElementUtils.findMergedAnnotation(current.getClass(), ResponseStatus.class);

      if (responseStatus != null) {
        return responseStatus;
      }

      current = current.getCause();
    }

    return null;
  }
}
