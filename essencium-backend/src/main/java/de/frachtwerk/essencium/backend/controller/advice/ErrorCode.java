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

import org.springframework.http.HttpStatusCode;

/** The constructor argument is the public wire value of {@code ProblemDetail.type}. */
public enum ErrorCode implements ProblemErrorCode {
  NOT_FOUND("NOT_FOUND"),
  INVALID_INPUT("INVALID_INPUT"),
  DUPLICATE_RESOURCE("DUPLICATE_RESOURCE"),
  FORBIDDEN("FORBIDDEN"),
  TOKEN_INVALIDATION("TOKEN_INVALIDATION"),
  TRANSLATION_FILE_ERROR("TRANSLATION_FILE_ERROR"),
  VALIDATION_FAILED("VALIDATION_FAILED"),
  MALFORMED_REQUEST("MALFORMED_REQUEST"),
  UNIQUE_CONSTRAINT_VIOLATION("UNIQUE_CONSTRAINT_VIOLATION"),
  FOREIGN_KEY_VIOLATION("FOREIGN_KEY_VIOLATION"),
  NOT_NULL_VIOLATION("NOT_NULL_VIOLATION"),
  DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION"),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
  AUTHENTICATION_FAILED("AUTHENTICATION_FAILED"),
  METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED"),
  NOT_ACCEPTABLE("NOT_ACCEPTABLE"),
  PAYLOAD_TOO_LARGE("PAYLOAD_TOO_LARGE"),
  UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE"),
  CONFLICT("CONFLICT"),
  CLIENT_ERROR("CLIENT_ERROR");

  private final String code;

  ErrorCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  /**
   * Code for an exception that carries a status but no code of its own, so that a {@code
   * ResponseStatusException} and a domain exception report the same code for the same condition.
   */
  public static ErrorCode forStatus(HttpStatusCode statusCode) {
    if (statusCode.is5xxServerError()) {
      return INTERNAL_SERVER_ERROR;
    }

    return switch (statusCode.value()) {
      case 400 -> INVALID_INPUT;
      case 401 -> AUTHENTICATION_FAILED;
      case 403 -> FORBIDDEN;
      case 404 -> NOT_FOUND;
      case 405 -> METHOD_NOT_ALLOWED;
      case 406 -> NOT_ACCEPTABLE;
      case 409 -> CONFLICT;
      case 413 -> PAYLOAD_TOO_LARGE;
      case 415 -> UNSUPPORTED_MEDIA_TYPE;
      default -> CLIENT_ERROR;
    };
  }
}
