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

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoleInitializer.class);

  private final RoleService roleService;
  private final RoleRepository roleRepository;
  private final RightService rightService;
  private final InitProperties initProperties;

  @Override
  public int order() {
    return 30;
  }

  protected Collection<Right> getAdminRights() {
    // admin rights will be reset to BasicApplicationRights on every startup
    return Arrays.stream(BasicApplicationRight.values())
        .map(BasicApplicationRight::getAuthority)
        .map(rightService::getByAuthority)
        .collect(Collectors.toSet());
  }

  /**
   * @deprecated since 2.5.0, for removal in 3.0.0. Use configuration properties 'essencium.init'
   *     instead.
   */
  @Deprecated(since = "2.5.0", forRemoval = true)
  protected Collection<Right> getUserRights() {
    return List.of();
  }

  /**
   * @deprecated since 2.5.0, for removal in 3.0.0. Use configuration properties 'essencium.init'
   *     instead.
   */
  @Deprecated(since = "2.5.0", forRemoval = true)
  protected Collection<Role> getAdditionalRoles() {
    return Set.of();
  }

  @Override
  public void run() {
    long markedAsDefaultRole =
        initProperties.getRoles().stream().filter(RoleProperties::isDefaultRole).count();
    boolean defaultRoleExists = markedAsDefaultRole > 0;
    if (markedAsDefaultRole > 1) {
      throw new IllegalStateException("More than one role is marked as default role");
    }

    Set<Role> existingRoles = new HashSet<>(roleService.getAll());

    initProperties
        .getRoles()
        .forEach(
            roleProperties -> {
              if (roleProperties.getName().equals("ADMIN")) {
                roleProperties
                    .getRights()
                    .addAll(
                        getAdminRights().stream()
                            .map(Right::getAuthority)
                            .collect(Collectors.toSet()));
                if (!defaultRoleExists) {
                  roleProperties.setDefaultRole(true);
                }
              }
              existingRoles.stream()
                  .filter(role -> role.getName().equals(roleProperties.getName()))
                  .findAny()
                  .ifPresentOrElse(
                      role -> updateExistingRole(roleProperties, role),
                      () -> createNewRole(roleProperties));
            });

    // remove System role flag from all roles that are not provided by the environment
    existingRoles.stream()
        .filter(Role::isSystemRole)
        .filter(
            role ->
                initProperties.getRoles().stream()
                    .noneMatch(roleProperties -> roleProperties.getName().equals(role.getName())))
        .forEach(
            role -> {
              role.setSystemRole(false);
              roleService.save(role);
              LOGGER.info("Removed system role flag from role [{}]", role.getName());
            });
  }

  private void updateExistingRole(RoleProperties roleProperties, Role role) {
    role.setDescription(roleProperties.getDescription());
    role.setProtected(roleProperties.isProtected());
    role.setDefaultRole(roleProperties.isDefaultRole());
    role.setSystemRole(true);
    role.setRights(
        roleProperties.getRights().stream()
            .map(rightService::getByAuthority)
            .collect(Collectors.toSet()));
    // roleRepository has to be used here because some existing roles might be
    // overwritten by the environment
    roleRepository.save(role);
    LOGGER.info("Updated role [{}]", role.getName());
  }

  private void createNewRole(RoleProperties roleProperties) {
    roleService.save(
        Role.builder()
            .name(roleProperties.getName())
            .description(roleProperties.getDescription())
            .isProtected(roleProperties.isProtected())
            .isDefaultRole(roleProperties.isDefaultRole())
            .isSystemRole(true)
            .rights(
                roleProperties.getRights().stream()
                    .map(rightService::getByAuthority)
                    .collect(Collectors.toSet()))
            .build());
    LOGGER.info("Created role [{}]", roleProperties.getName());
  }
}
