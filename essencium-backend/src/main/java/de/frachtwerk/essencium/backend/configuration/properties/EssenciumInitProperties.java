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

package de.frachtwerk.essencium.backend.configuration.properties;

import de.frachtwerk.essencium.backend.configuration.properties.embedded.RoleProperties;
import de.frachtwerk.essencium.backend.configuration.properties.embedded.UserProperties;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.init")
public class EssenciumInitProperties {
  private Set<UserProperties> users = new HashSet<>();
  private Set<RoleProperties> roles = new HashSet<>();

  public Set<RoleProperties> getRoles() {
    if (roles.stream().noneMatch(role -> role.getName().equals("ADMIN"))) {
      roles.add(new RoleProperties());
    }
    return roles;
  }
}
