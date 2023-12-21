/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @deprecated Deprecated. Use one of the known subclasses of AuthenticationException instead.
 * @see <a
 *     href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html">https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html</a>
 */
@Deprecated(since = "2.5.0", forRemoval = true)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends AuthenticationException {

  public UnauthorizedException(final String message) {
    super(message);
  }

  public UnauthorizedException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
