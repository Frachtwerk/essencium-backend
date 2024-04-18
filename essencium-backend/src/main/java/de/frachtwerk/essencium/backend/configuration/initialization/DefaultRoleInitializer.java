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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.configuration.properties.RoleProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DefaultRoleInitializer implements DataInitializer {
  public static final String DEFAULT_ADMIN_ROLE_NAME = "ADMIN";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoleInitializer.class);

  private final InitProperties initProperties;

  private final RoleRepository roleRepository;
  protected final RoleService roleService;
  protected final RightService rightService;

  protected Map<String, Right> rightCache;

  @Override
  public int order() {
    return 30;
  }

  protected Collection<Right> getAdminRights() {
    // admin rights will be reset to BasicApplicationRights on every startup
    return Arrays.stream(BasicApplicationRight.values())
        .map(BasicApplicationRight::getAuthority)
        .map(rightCache::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(HashSet::new));
  }

  protected Collection<Role> getAdditionalRoles() {
    return Set.of();
  }

  @Override
  public void run() {
    rightCache =
        rightService.getAll().stream()
            .collect(Collectors.toMap(Right::getAuthority, right -> right));
    // create roles from properties
    HashSet<Role> roles =
        initProperties.getRoles().stream()
            .map(this::getRoleFromProperties)
            .collect(Collectors.toCollection(HashSet::new));

    // add additional roles defined during development
    roles.addAll(getAdditionalRoles());

    // Ensure that there is at least one role with all BasicApplicationRights
    Collection<Right> adminRights = getAdminRights();
    if (!hasAdminRights(roles)) {
      roles.stream()
          .filter(role -> role.getName().equals(DEFAULT_ADMIN_ROLE_NAME))
          .findAny()
          .ifPresentOrElse(
              role -> role.getRights().addAll(adminRights),
              () ->
                  roles.add(
                      Role.builder()
                          .name(DEFAULT_ADMIN_ROLE_NAME)
                          .description("Administrator")
                          .isProtected(true)
                          .isSystemRole(true)
                          .rights(new HashSet<>(adminRights))
                          .build()));
    }

    // Validate that there is only one default role
    if (roles.stream().filter(Role::isDefaultRole).count() > 1) {
      throw new IllegalStateException("More than one role is marked as default role");
    }
    if (roles.stream().noneMatch(Role::isDefaultRole)) {
      throw new IllegalStateException("No role is marked as default role");
    }

    Set<Role> existingRoles = new HashSet<>(roleService.getAll());

    roles.forEach(
        newRole ->
            existingRoles.stream()
                .filter(role -> role.getName().equals(newRole.getName()))
                .findAny()
                .ifPresentOrElse(
                    role -> updateExistingRole(newRole, role), () -> createNewRole(newRole)));

    // Remove System role flag and protected flag from all roles that are not provided by the
    // environment or the application. This allows application administrators to delete obsolete
    // roles when they are no longer needed.
    existingRoles.stream()
        .filter(Role::isSystemRole)
        .filter(
            existingRole ->
                roles.stream().noneMatch(role -> role.getName().equals(existingRole.getName())))
        .forEach(
            role -> {
              role.setSystemRole(false);
              role.setProtected(false);
              roleRepository.save(role);
              LOGGER.info("Removed system role flag from role [{}]", role.getName());
            });
  }

  public boolean hasAdminRights(Set<Role> roles) {
    // CAUTION: the admin rights cannot be scattered over different roles
    return roles.stream().anyMatch(role -> role.getRights().containsAll(getAdminRights()));
  }

  Role getRoleFromProperties(RoleProperties roleProperties) {
    return Role.builder()
        .name(roleProperties.getName())
        .description(roleProperties.getDescription())
        .isProtected(roleProperties.isProtected())
        .isDefaultRole(roleProperties.isDefaultRole())
        .isSystemRole(true)
        .rights(
            roleProperties.getRights().stream()
                .map(rightCache::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
        .build();
  }

  void updateExistingRole(Role newRole, Role role) {
    role.setDescription(newRole.getDescription());
    role.setProtected(newRole.isProtected());
    role.setDefaultRole(newRole.isDefaultRole());
    role.setSystemRole(true);
    role.setRights(
        newRole.getRights().stream()
            .map(Right::getAuthority)
            .map(rightCache::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
    roleRepository.save(role);
    LOGGER.info("Updated role [{}]", role.getName());
  }

  private void createNewRole(Role newRole) {
    roleRepository.save(newRole);
    LOGGER.info("Created role [{}]", newRole.getName());
  }
}
