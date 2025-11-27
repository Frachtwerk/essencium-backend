/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

public enum AdditionalApplicationRights implements GrantedAuthority, EssenciumApplicationRight {
  API_DEVELOPER(AdditionalApplicationRights.Authority.API_DEVELOPER, ""),
  API_TOKEN(AdditionalApplicationRights.Authority.API_TOKEN, ""),
  API_TOKEN_ADMIN(AdditionalApplicationRights.Authority.API_TOKEN_ADMIN, ""),
  SESSION_TOKEN_ADMIN(AdditionalApplicationRights.Authority.SESSION_TOKEN_ADMIN, "");

  @Getter private final String authority;
  @Getter private final String description;

  AdditionalApplicationRights(@NotNull final String authority, @NotNull final String description) {
    this.authority = authority;
    this.description = description;
  }

  public static class Authority {
    public static final String API_DEVELOPER = "API_DEVELOPER";
    public static final String API_TOKEN = "API_TOKEN";
    public static final String API_TOKEN_ADMIN = "API_TOKEN_ADMIN";
    public static final String SESSION_TOKEN_ADMIN = "SESSION_TOKEN_ADMIN";
  }
}
