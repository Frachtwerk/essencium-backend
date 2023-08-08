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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.DefaultRoleProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

@Configuration
public class DefaultRoleInitializer implements DataInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoleInitializer.class);

  public static final String ADMIN_ROLE_NAME = "ADMIN";
  public static final String ADMIN_ROLE_DESCRIPTION = "Application Admin";

  protected final DefaultRoleProperties defaultRoleProperties;

  protected final RightService rightService;
  protected final RoleService roleService;

  @Autowired
  public DefaultRoleInitializer(
      @NotNull final RightService rightService,
      @NotNull final RoleService roleService,
      final DefaultRoleProperties defaultRoleProperties) {
    this.rightService = rightService;
    this.roleService = roleService;
    this.defaultRoleProperties = defaultRoleProperties;
  }

  protected Collection<Right> getAdminRights() {
    return rightService.getAll();
  }

  protected Collection<Right> getUserRights() {
    return List.of();
  }

  protected RoleDto getAdminRole() {
    final Set<String> rights =
        getAdminRights().stream().map(Right::getAuthority).collect(Collectors.toSet());

    return new RoleDto(ADMIN_ROLE_NAME, ADMIN_ROLE_DESCRIPTION, rights, true);
  }

  protected RoleDto getUserRole() {
    final Set<String> rights =
        getUserRights().stream().map(Right::getAuthority).collect(Collectors.toSet());

    return new RoleDto(
        defaultRoleProperties.getName(), defaultRoleProperties.getDescription(), rights, false);
  }

  protected Collection<RoleDto> getAdditionalRoles() {
    return Set.of();
  }

  @Override
  public void run() {
    final RoleDto adminRole = getAdminRole();
    final RoleDto userRole = getUserRole();
    final Collection<RoleDto> additionalRoles = getAdditionalRoles();
    final Map<String, Role> existingRoles =
        roleService.getAll().stream().collect(Collectors.toMap(Role::getName, Function.identity()));

    Stream.concat(Stream.of(adminRole, userRole), additionalRoles.stream())
        .map(r -> Pair.of(r, Optional.ofNullable(existingRoles.getOrDefault(r.getName(), null))))
        .forEach(
            p -> {
              final var role = p.getFirst();
              p.getSecond()
                  .ifPresentOrElse(
                      existingRole -> {
                        if ((existingRole.isProtected() || role.isProtected())
                            && !existingRole.equalsDto(role)) {
                          // if the existing role is protected it must be overwritten by the new
                          // role
                          // if the new role is protected, it must overwrite the existing role
                          LOGGER.info(
                              "Overwriting protected role [{}] with {} rights",
                              role.getName(),
                              role.getRights().size());
                          role.setName(existingRole.getName());
                          roleService.update(existingRole.getName(), role);
                        } else {
                          LOGGER.info(
                              "Skipping existing role [{}] with {} existing rights",
                              existingRole.getName(),
                              existingRole.getRights().size());
                        }
                      },
                      () -> {
                        LOGGER.info(
                            "Initializing role [{}] with {} rights",
                            role.getName(),
                            role.getRights().size());
                        roleService.create(role);
                      });
            });
  }

  @Override
  public int order() {
    return 30;
  }
}
