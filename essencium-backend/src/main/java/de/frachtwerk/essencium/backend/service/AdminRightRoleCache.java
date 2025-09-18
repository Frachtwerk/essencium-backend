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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AdminRightRoleCache {

  private final Set<Role> adminRoles = new HashSet<>();

  private final Set<Right> adminRights = new HashSet<>();

  private final RightRepository rightRepository;
  private final RoleRepository roleRepository;

  public AdminRightRoleCache(RightRepository rightRepository, RoleRepository roleRepository) {
    this.rightRepository = rightRepository;
    this.roleRepository = roleRepository;
  }

  public Set<Role> getAdminRoles() {
    if (adminRoles.isEmpty()) {
      adminRoles.addAll(
          roleRepository.findAll().stream()
              .filter(role -> role.getRights().containsAll(getAdminRights()))
              .collect(Collectors.toSet()));
    }

    return Set.copyOf(adminRoles);
  }

  public Set<Right> getAdminRights() {
    if (adminRights.isEmpty()) {
      adminRights.addAll(
          // admin rights will be reset to BasicApplicationRights on every startup
          Arrays.stream(BasicApplicationRight.values())
              .map(BasicApplicationRight::getAuthority)
              .map(rightRepository::findById)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toCollection(HashSet::new)));
    }

    return Set.copyOf(adminRights);
  }

  public void reset() {
    this.adminRoles.clear();
    this.adminRights.clear();
  }

  public boolean isEmpty() {
    return this.adminRoles.isEmpty() && this.adminRights.isEmpty();
  }
}
