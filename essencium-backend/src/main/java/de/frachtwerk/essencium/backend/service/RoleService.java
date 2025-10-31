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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

  private final RoleRepository roleRepository;
  private final AdminRightRoleCache adminRightRoleCache;

  @Setter
  protected AbstractUserService<
          ? extends AbstractBaseUser<?>,
          ? extends EssenciumUserDetails<?>,
          ? extends Serializable,
          ? extends BaseUserDto<?>>
      userService;

  public List<Role> getAll() {
    return roleRepository.findAll();
  }

  public Page<Role> getAll(Pageable pageable) {
    return roleRepository.findAll(pageable);
  }

  public Role getByName(String name) {
    return roleRepository.findByName(name);
  }

  public Role save(RoleDto roleDto) {
    return save(roleDto.toRole());
  }

  public Role save(Role role) {
    Optional<Role> existingRole = roleRepository.findById(role.getName());
    if (existingRole.isPresent()) {
      if (existingRole.get().isProtected()) {
        throw new NotAllowedException("Protected roles cannot be updated");
      }
      if (!Objects.equals(existingRole.get().isSystemRole(), role.isSystemRole())) {
        throw new NotAllowedException("System defined roles cannot be changed");
      }
    }
    if (role.isDefaultRole()) {
      roleRepository
          .findByIsDefaultRoleIsTrue()
          .ifPresent(
              existingDefaultRole -> {
                if (!Objects.equals(existingDefaultRole.getName(), role.getName())) {
                  throw new ResourceUpdateException(
                      "There is already a default role ("
                          + existingDefaultRole.getName()
                          + ") set");
                }
              });
    }

    adminRightRoleCache.reset();

    return roleRepository.save(role);
  }

  public final void deleteById(@NotNull final String id) {
    delete(Role.builder().name(id).build());
  }

  public void delete(Role role) {
    Role existingRole =
        roleRepository.findById(role.getName()).orElseThrow(ResourceNotFoundException::new);
    if (existingRole.isProtected()) {
      throw new NotAllowedException("Protected roles cannot be deleted");
    }

    if (!userService.loadUsersByRole(existingRole.getName()).isEmpty()) {
      throw new NotAllowedException("There are Users assigned to this Role");
    }

    adminRightRoleCache.reset();
    roleRepository.delete(role);
  }

  @NotNull
  public final Role patch(
      @NotNull final String id, @NotNull final Map<String, Object> fieldUpdates) {
    Role existingRole = roleRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (existingRole.isProtected())
      throw new NotAllowedException("Protected roles cannot be updated");
    RoleDto roleDto =
        RoleDto.builder()
            .name(existingRole.getName())
            .description(existingRole.getDescription())
            .isProtected(existingRole.isProtected())
            .isDefaultRole(existingRole.isDefaultRole())
            .rights(
                existingRole.getRights().stream()
                    .map(Right::getAuthority)
                    .collect(Collectors.toSet()))
            .build();
    fieldUpdates.forEach(
        (key, value) -> {
          switch (key) {
            case "name" -> throw new ResourceUpdateException("Name cannot be updated");
            case "description" -> roleDto.setDescription((String) value);
            case "isProtected" -> roleDto.setProtected((boolean) value);
            case "isDefaultRole" -> roleDto.setDefaultRole((boolean) value);
            case "rights" -> roleDto.setRights(getPatchRights(value));
            default -> log.warn("Unknown field [{}] for patching", key);
          }
        });
    return save(roleDto);
  }

  private Set<Object> getPatchRights(Object value) {
    if (value instanceof Collection<?> collection) {
      if (collection.stream().allMatch(String.class::isInstance)) {
        return collection.stream().map(String.class::cast).collect(Collectors.toSet());
      } else if (collection.stream().allMatch(Right.class::isInstance)) {
        return collection.stream()
            .map(Right.class::cast)
            .map(Right::getAuthority)
            .collect(Collectors.toSet());
      } else {
        throw new ResourceUpdateException("Rights must be a set of Strings or Rights");
      }
    } else {
      throw new ResourceUpdateException("Rights must be a set of Strings or Rights");
    }
  }

  public Role getDefaultRole() {
    return roleRepository.findByIsDefaultRoleIsTrue().orElse(null);
  }

  public Collection<Role> getByRight(String rightId) {
    return roleRepository.findAllByRights_Authority(rightId);
  }
}
