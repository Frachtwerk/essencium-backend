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

public class TokenException extends EssenciumRuntimeException {

  public static final String REFRESH_TOKEN = "refresh toke";
  public static final String AUTHENTICATION_TOKEN = "authentication token";
  public static final String SESSION_TOKEN = "session token";

  public static final String TOKEN_STATE_MISSING = "missing";
  public static final String TOKEN_STATE_EXPIRED = "expired";
  public static final String TOKEN_STATE_INVALID = "invalid";

  private final String type;
  private final String state;

  public TokenException(String type, String state, String message) {
    super(message);
    this.type = type;
    this.state = state;
  }

  @Override
  public Map<String, Object> reportInternals() {
    HashMap<String, Object> internals = new HashMap<>(super.reportInternals());
    internals.put("tokenType", type);
    internals.put("tokenState", state);
    return internals;
  }
}
