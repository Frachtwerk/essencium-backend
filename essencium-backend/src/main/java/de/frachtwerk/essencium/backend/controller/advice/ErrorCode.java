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

public enum ErrorCode {
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
  DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION");

  private final String code;

  ErrorCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
