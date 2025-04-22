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

import java.util.HashMap;
import java.util.Map;

public class MissingDataException extends EssenciumRuntimeException {

  private final Map<String, String> requiredData;

  public MissingDataException(String message) {
    super(message);
    this.requiredData = new HashMap<>();
  }

  public MissingDataException(String message, Throwable cause) {
    super(message, cause);
    this.requiredData = new HashMap<>();
  }

  public MissingDataException(Map<String, String> requiredData) {
    super("Missing required data.");
    this.requiredData = requiredData;
  }

  public MissingDataException(String message, Map<String, String> requiredData) {
    super(message);
    this.requiredData = requiredData;
  }

  public MissingDataException(String message, Map<String, String> requiredData, Throwable cause) {
    super(message, cause);
    this.requiredData = requiredData;
  }

  private String convertEntryToString(Map.Entry<String, String> entry) {
    return entry.getKey() + " (" + entry.getValue() + ")";
  }

  @Override
  public String getMessage() {
    if (this.requiredData.isEmpty()) {
      return super.getMessage();
    }
    String baseMessage = "Following required data is missing: ";
    return baseMessage
        + String.join(
            ", ", requiredData.entrySet().stream().map(this::convertEntryToString).toList());
  }

  @Override
  public Map<String, Object> reportInternals() {
    HashMap<String, Object> result = new HashMap<>(super.reportInternals());
    result.put(
        "requiredData", requiredData.entrySet().stream().map(this::convertEntryToString).toList());
    return result;
  }
}
