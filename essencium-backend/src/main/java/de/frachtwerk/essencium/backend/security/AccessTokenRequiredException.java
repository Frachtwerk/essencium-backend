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

package de.frachtwerk.essencium.backend.security;

import org.springframework.security.authentication.AuthenticationServiceException;

/**
 * Thrown when a protected endpoint requires an access token but a different token type (e.g. API
 * token or refresh token) was presented. Results in HTTP 403 Forbidden because the client is
 * authenticated, but not authorized to use this endpoint with the given credential type.
 */
public class AccessTokenRequiredException extends AuthenticationServiceException {

  public AccessTokenRequiredException(String message) {
    super(message);
  }
}
