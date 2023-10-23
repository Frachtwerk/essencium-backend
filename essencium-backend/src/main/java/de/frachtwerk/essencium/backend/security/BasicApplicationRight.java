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

package de.frachtwerk.essencium.backend.security;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

public enum BasicApplicationRight implements GrantedAuthority {
  API_DEVELOPER(Authority.API_DEVELOPER, ""),
  USER_CREATE(Authority.USER_CREATE, ""),
  USER_READ(Authority.USER_READ, ""),
  USER_UPDATE(Authority.USER_UPDATE, ""),
  USER_DELETE(Authority.USER_DELETE, ""),
  ROLE_CREATE(Authority.ROLE_CREATE, ""),
  ROLE_READ(Authority.ROLE_READ, ""),
  ROLE_UPDATE(Authority.ROLE_UPDATE, ""),
  ROLE_DELETE(Authority.ROLE_DELETE, ""),
  RIGHT_READ(Authority.RIGHT_READ, ""),
  RIGHT_UPDATE(Authority.RIGHT_UPDATE, ""),
  TRANSLATION_CREATE(Authority.TRANSLATION_CREATE, ""),
  TRANSLATION_READ(Authority.TRANSLATION_READ, ""),
  TRANSLATION_UPDATE(Authority.TRANSLATION_UPDATE, ""),
  TRANSLATION_DELETE(Authority.TRANSLATION_DELETE, "");

  @Getter private final String authority;
  @Getter private final String description;

  BasicApplicationRight(@NotNull final String authority, @NotNull final String description) {
    this.authority = authority;
    this.description = description;
  }

  public static class Authority {
    public static final String API_DEVELOPER = "API_DEVELOPER";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_READ = "USER_READ";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_DELETE = "ROLE_DELETE";
    public static final String RIGHT_READ = "RIGHT_READ";
    public static final String RIGHT_UPDATE = "RIGHT_UPDATE";
    public static final String TRANSLATION_CREATE = "TRANSLATION_CREATE";
    public static final String TRANSLATION_READ = "TRANSLATION_READ";
    public static final String TRANSLATION_UPDATE = "TRANSLATION_UPDATE";
    public static final String TRANSLATION_DELETE = "TRANSLATION_DELETE";
  }
}
