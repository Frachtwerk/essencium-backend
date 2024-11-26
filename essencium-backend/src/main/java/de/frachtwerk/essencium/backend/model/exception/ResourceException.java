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
import java.util.Objects;
import lombok.Getter;

@Getter
public class ResourceException extends EssenciumRuntimeException implements ReportableException {

  private final String resourceType;
  private final String action;
  private final String identifier;

  public static final String UNKNOWN_TYPE = "[unknown]";

  public ResourceException(String message) {
    super(message);
    resourceType = UNKNOWN_TYPE;
    action = UNKNOWN_TYPE;
    identifier = UNKNOWN_TYPE;
  }

  public ResourceException(String message, Throwable cause) {
    super(message, cause);
    resourceType = UNKNOWN_TYPE;
    action = UNKNOWN_TYPE;
    identifier = UNKNOWN_TYPE;
  }

  public ResourceException(String message, String resourceType, String action, String identifier) {
    super(message);
    this.resourceType = resourceType != null ? resourceType : UNKNOWN_TYPE;
    this.action = action;
    this.identifier = identifier;
  }

  public ResourceException(
      String message, String resourceType, String action, String identifier, Throwable cause) {
    super(message, cause);
    this.resourceType = resourceType != null ? resourceType : UNKNOWN_TYPE;
    this.action = action;
    this.identifier = identifier;
  }

  @Override
  public String getMessage() {
    String baseMessage =
        String.format(
            "Error during '%s' action on resource with identifier '%s': %s",
            action, identifier, super.getMessage());

    if (!Objects.equals(resourceType, UNKNOWN_TYPE)) {
      return String.format(
          "Error during '%s' action on resource type '%s' with identifier '%s': %s",
          action, resourceType, identifier, super.getMessage());
    }

    return baseMessage;
  }

  @Override
  public Map<String, Object> reportInternals() {
    HashMap<String, Object> result = new HashMap<>(super.reportInternals());
    result.put("resourceType", resourceType);
    result.put("action", action);
    result.put("identifier", identifier);
    return result;
  }
}
