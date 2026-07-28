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

package de.frachtwerk.essencium.backend.configuration.properties;

import de.frachtwerk.essencium.backend.configuration.properties.embedded.RoleProperties;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bootstrap/seed data applied on application startup, bound from the {@code essencium.init.*}
 * namespace.
 *
 * <p>The initializers use these values to create the initial set of roles and users when the
 * database is still empty. They are typically only relevant on first launch; on subsequent starts
 * existing entities are not overwritten.
 *
 * <p>Correlation: users reference roles by name, so any role assigned to an initial user should
 * also be declared in {@link #roles} (or be one of the built-in roles).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.init")
public class EssenciumInitProperties {
  /**
   * Initial users to create on startup. Each entry is a free-form key/value map describing a user
   * (e.g. {@code email}, {@code firstName}, {@code lastName}, {@code roles}); the concrete keys
   * depend on the application's user model. Defaults to an empty set (no seed users).
   */
  private Set<Map<String, Object>> users = new HashSet<>();

  /**
   * Initial roles to create on startup, described via {@link RoleProperties}. Defaults to an empty
   * set. Note that {@link #getRoles()} guarantees a protected {@code ADMIN} role is always present,
   * so an {@code ADMIN} role is implicitly added even if it is not configured here.
   */
  private Set<RoleProperties> roles = new HashSet<>();

  /**
   * Returns the configured roles, ensuring that a default {@code ADMIN} role (see {@link
   * RoleProperties}) is always present even when none was configured.
   */
  public Set<RoleProperties> getRoles() {
    if (roles.stream().noneMatch(role -> role.getName().equals("ADMIN"))) {
      roles.add(new RoleProperties());
    }
    return roles;
  }
}
