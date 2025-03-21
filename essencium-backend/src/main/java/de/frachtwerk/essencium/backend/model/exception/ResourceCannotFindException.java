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

@SuppressWarnings("java:S110")
public class ResourceCannotFindException extends ResourceException {

  private static final String DEFAULT_ACTION = "FIND";
  private static final String DEFAULT_MESSAGE = "Cannot find resource with ID '%s'";

  public ResourceCannotFindException(String resourceType, String identifier) {
    super(String.format(DEFAULT_MESSAGE, identifier), resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotFindException(String identifier) {
    super(String.format(DEFAULT_MESSAGE, identifier), "Unknown", DEFAULT_ACTION, identifier);
  }

  public ResourceCannotFindException(String resourceType, String identifier, Throwable cause) {
    super(
        String.format(DEFAULT_MESSAGE, identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier,
        cause);
  }

  public ResourceCannotFindException(String message, String resourceType, String identifier) {
    super(message, resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotFindException(
      String message, String resourceType, String identifier, Throwable cause) {
    super(message, resourceType, DEFAULT_ACTION, identifier, cause);
  }
}
