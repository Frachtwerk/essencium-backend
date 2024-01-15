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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {

  private static final Logger LOG = LoggerFactory.getLogger(RoleService.class);

  private final RoleRepository roleRepository;
  private final RightRepository rightRepository;

  @Setter
  protected AbstractUserService<
          ? extends AbstractBaseUser<?>, ? extends Serializable, ? extends UserDto<?>>
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
    return roleRepository.save(role);
  }

  public void delete(Role role) {
    Optional<Role> byId = roleRepository.findById(role.getName());
    if (byId.isPresent()) {
      if (byId.get().isProtected()) {
        throw new NotAllowedException("Protected roles cannot be deleted");
      }
      if (!userService.loadUsersByRole(role.getName()).isEmpty()) {
        throw new NotAllowedException("There are Users assigned to this Role");
      }
      roleRepository.delete(role);
    }
  }

  /**
   * @deprecated Use {@link #getByName(String)} instead.
   * @param id {@link Role#getName()}
   * @return {@link Role}
   */
  @NotNull
  @Deprecated(since = "2.5.0", forRemoval = true)
  public final Role getById(@NotNull final String id) {
    return getByName(id);
  }

  /**
   * @deprecated Use {@link #save(Role)} instead.
   * @param role {@link Role}
   * @return {@link Role}
   */
  @NotNull
  @Deprecated(since = "2.5.0", forRemoval = true)
  public final Role create(Role role) {
    return save(role);
  }

  /**
   * @deprecated Use {@link #save(Role)} instead.
   * @param name {@link Role#getName()}
   * @param entity {@link Role}
   * @return {@link Role}
   */
  @NotNull
  @Deprecated(since = "2.5.0", forRemoval = true)
  public final Role update(@NotNull final String name, @NotNull final Role entity) {
    if (!Objects.equals(entity.getName(), name)) {
      throw new ResourceUpdateException("Name needs to match entity name");
    }
    if (!roleRepository.existsById(name)) {
      throw new ResourceNotFoundException("Entity to update is not persistent");
    }
    return save(entity);
  }

  @NotNull
  public final Role patch(
      @NotNull final String id, @NotNull final Map<String, Object> fieldUpdates) {
    Role existingRole = roleRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (existingRole.isProtected()) {
      throw new NotAllowedException("Protected roles cannot be updated");
    }
    fieldUpdates.forEach(
        (key, value) -> {
          switch (key) {
            case "name":
              throw new ResourceUpdateException("Name cannot be updated");
            case "description":
              existingRole.setDescription((String) value);
              break;
            case "isProtected":
              existingRole.setProtected((boolean) value);
              break;
            case "isDefaultRole":
              patchIsDefaultRole((boolean) value, existingRole);
              break;
            case "rights":
              patchRights(value, existingRole);
              break;
            default:
              LOG.warn("Unknown field [{}] for patching", key);
          }
        });
    return roleRepository.save(existingRole);
  }

  private void patchIsDefaultRole(boolean value, Role existingRole) {
    if (value) {
      roleRepository
          .findByIsDefaultRoleIsTrue()
          .ifPresent(
              role -> {
                throw new ResourceUpdateException(
                    "There is already a default role (" + role.getName() + ") set");
              });
    }
    existingRole.setDefaultRole(value);
  }

  private void patchRights(Object value, Role existingRole) {
    if (value instanceof Set<?>) {
      Set<Right> rights;
      if (((Set<?>) value).stream().allMatch(String.class::isInstance)) {
        //noinspection unchecked
        rights =
            ((Set<String>) value)
                .stream().map(rightRepository::findByAuthority).collect(Collectors.toSet());
      } else if (((Set<?>) value).stream().allMatch(Right.class::isInstance)) {
        // noinspection unchecked
        rights =
            ((Set<Right>) value)
                .stream()
                    .map(Right::getAuthority)
                    .map(rightRepository::findByAuthority)
                    .collect(Collectors.toSet());
      } else {
        throw new ResourceUpdateException("Rights must be a set of Strings or Rights");
      }
      existingRole.setRights(rights);
    } else {
      throw new ResourceUpdateException("Rights must be a set of Strings or Rights");
    }
  }

  public final void deleteById(@NotNull final String id) {
    Role role = roleRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (!userService.loadUsersByRole(role.getName()).isEmpty()) {
      throw new NotAllowedException("There are Users assigned to this Role");
    }
    roleRepository.delete(role);
  }

  /**
   * @deprecated Use {@link #getByName(String)} instead.
   * @param roleName {@link Role#getName()}
   * @return {@link Role}
   */
  @Deprecated(since = "2.5.0", forRemoval = true)
  public Role getRole(@NotNull final String roleName) {
    return getByName(roleName);
  }

  public Role getDefaultRole() {
    return roleRepository.findByIsDefaultRoleIsTrue().orElse(null);
  }

  public Collection<Role> getByRight(String rightId) {
    return roleRepository.findAllByRights_Authority(rightId);
  }
}
