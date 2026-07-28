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

package de.frachtwerk.essencium.backend.configuration.properties.embedded;

import static de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer.DEFAULT_ADMIN_ROLE_NAME;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description of a single role to be created on startup, embedded in {@link
 * de.frachtwerk.essencium.backend.configuration.properties.EssenciumInitProperties#getRoles()}.
 *
 * <p>The default values describe the built-in protected {@code ADMIN} role that Essencium always
 * ensures exists; a configured role typically overrides {@link #name}, {@link #description} and
 * {@link #rights}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleProperties {
  /** Unique role name. Default: the built-in admin role name ({@code ADMIN}). */
  private String name = DEFAULT_ADMIN_ROLE_NAME;

  /** Human-readable description of the role. Default: {@code "Administrator"}. */
  private String description = "Administrator";

  /** Set of right/authority identifiers granted to this role. Default: empty. */
  private Set<String> rights = new HashSet<>();

  /**
   * Whether the role is protected against deletion/modification via the API. Default: {@code true}
   * (matching the built-in admin role).
   */
  private boolean isProtected = true;

  /**
   * Whether this is the default role automatically assigned to newly created users. Default: {@code
   * false}. At most one role should have this set to {@code true}.
   */
  private boolean isDefaultRole = false;
}
