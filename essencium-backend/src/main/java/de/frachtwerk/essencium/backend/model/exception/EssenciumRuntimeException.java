/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.model.exception;

import java.util.Arrays;
import java.util.Map;

public class EssenciumRuntimeException extends RuntimeException implements ReportableException {
  public EssenciumRuntimeException(String message) {
    super(message);
  }

  public EssenciumRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public Map<String, Object> reportInternals() {
    return Map.of(
        "internalErrorType", this.getClass().getSimpleName(), "internalErrorMessage", getMessage());
  }

  @Override
  public Map<String, Object> reportDebug() {
    return Map.of(
        "stackTrace",
        Arrays.stream(this.getStackTrace()).map(StackTraceElement::toString).toList());
  }
}
