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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumInitProperties;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.AdminRightRoleCache;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
public class RoleInitializer extends DefaultRoleInitializer {
  public RoleInitializer(
      EssenciumInitProperties essenciumInitProperties,
      RoleRepository roleRepository,
      RoleService roleService,
      RightService rightService,
      AdminRightRoleCache adminRightRoleCache) {
    super(essenciumInitProperties, roleRepository, roleService, rightService, adminRightRoleCache);
  }

  @Override
  protected Collection<Role> getAdditionalRoles() {
    return List.of(
        Role.builder()
            .name("READER")
            .description("Read-only role with limited permissions")
            .rights(
                rightService.getAll().stream()
                    .filter(r -> r.getAuthority().equals("READ"))
                    .collect(Collectors.toSet()))
            .isDefaultRole(true)
            .isSystemRole(true)
            .build());
  }
}
