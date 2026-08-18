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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    log.error("Unhandled exception", exception);

    ProblemDetail problemDetail =
        problemDetailFactory.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR,
            "Internal server error",
            exception,
            request);

    return ResponseEntity.internalServerError().body(problemDetail);
  }
}
