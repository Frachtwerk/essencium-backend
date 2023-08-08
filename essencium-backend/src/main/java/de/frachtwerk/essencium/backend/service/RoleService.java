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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.configuration.properties.DefaultRoleProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  protected final RoleRepository repository;
  protected final RightRepository rightRepository;

  protected AbstractUserService<
          ? extends AbstractBaseUser<?>, ? extends Serializable, ? extends UserDto<?>>
      userService;
  private final DefaultRoleProperties defaultRoleProperties;

  public void setUserService(
      AbstractUserService<
              ? extends AbstractBaseUser<?>, ? extends Serializable, ? extends UserDto<?>>
          service) {
    this.userService = service;
  }

  @Autowired
  public RoleService(
      @NotNull final RoleRepository repository,
      RightRepository rightRepository,
      DefaultRoleProperties defaultRoleProperties) {
    this.repository = repository;
    this.rightRepository = rightRepository;
    this.defaultRoleProperties = defaultRoleProperties;
  }

  @NotNull
  public final List<Role> getAll() {
    final var allEntities = repository.findAll();
    return getAllPostProcessing(allEntities);
  }

  @NotNull
  public final Page<Role> getAll(@NotNull final Pageable pageable) {
    final var processedPageable = getAllPreProcessing(pageable);
    final var page = repository.findAll(processedPageable);
    return getAllPostProcessing(page);
  }

  @NotNull
  public final Role getById(@NotNull final String id) {
    final var processedId = getByIdPreProcessing(id);
    final var entity = repository.findById(processedId).orElseThrow(ResourceNotFoundException::new);
    return getByIdPostProcessing(entity);
  }

  @NotNull
  public final <E extends RoleDto> Role create(@NotNull final E entity) {
    final var processedEntity = createPreProcessing(entity);
    final var saved = repository.save(processedEntity);
    return createPostProcessing(saved);
  }

  @NotNull
  public final <E extends RoleDto> Role update(@NotNull final String id, @NotNull final E entity) {
    final var processedEntity = updatePreProcessing(id, entity);
    final var saved = repository.save(processedEntity);
    return updatePostProcessing(saved);
  }

  @NotNull
  public final Role patch(
      @NotNull final String id, @NotNull final Map<String, Object> fieldUpdates) {
    final var toUpdate = patchPreProcessing(id, fieldUpdates);
    final var saved = repository.save(toUpdate);
    return patchPostProcessing(saved);
  }

  public final void deleteById(@NotNull final String id) {
    deletePreProcessing(id);
    repository.deleteById(id);
    deletePostProcessing(id);
  }

  public Optional<Role> getRole(@NotNull final String roleName) {
    return repository.findByName(roleName);
  }

  public Optional<Role> getDefaultRole() {
    return getRole(defaultRoleProperties.getName());
  }

  public Collection<Role> getByRight(String rightId) {
    return repository.findByRights_Authority(rightId);
  }

  @NotNull
  protected List<Role> getAllPostProcessing(@NotNull List<Role> allEntities) {
    return allEntities.stream().map(this::postProcessing).toList();
  }

  @NotNull
  protected Pageable getAllPreProcessing(@NotNull final Pageable pageable) {
    return pageable;
  }

  @NotNull
  protected Page<Role> getAllPostProcessing(@NotNull final Page<Role> page) {
    return page.map(this::postProcessing);
  }

  @NotNull
  protected String getByIdPreProcessing(@NotNull final String id) {
    return id;
  }

  @NotNull
  protected Role getByIdPostProcessing(@NotNull final Role entity) {
    return postProcessing(entity);
  }

  @NotNull
  protected <E extends RoleDto> Role createPreProcessing(@NotNull final E entity) {
    return convertDtoToEntity(entity);
  }

  @NotNull
  protected Role createPostProcessing(@NotNull final Role saved) {
    return postProcessing(saved);
  }

  @NotNull
  protected <E extends RoleDto> Role updatePreProcessing(
      @NotNull final String id, @NotNull final E entity) {
    final Role entityToUpdate = convertDtoToEntity(entity);
    if (!Objects.equals(entityToUpdate.getName(), id)) {
      throw new ResourceUpdateException("Name needs to match entity name");
    }

    Optional<Role> currentEntityOpt = repository.findById(id);
    if (currentEntityOpt.isEmpty()) {
      throw new ResourceNotFoundException("Entity to update is not persistent");
    }
    return entityToUpdate;
  }

  @NotNull
  protected Role updatePostProcessing(@NotNull final Role saved) {
    return postProcessing(saved);
  }

  @NotNull
  protected Role patchPreProcessing(
      @NotNull final String id, @NotNull final Map<String, Object> fieldUpdates) {
    Role out = repository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (fieldUpdates.containsKey("name")) {
      throw new ResourceUpdateException("Name cannot be updated");
    }
    final Role toUpdate = out.clone();
    fieldUpdates.forEach((key, value) -> updateField(toUpdate, key, value));
    return toUpdate;
  }

  @NotNull
  protected Role patchPostProcessing(@NotNull final Role saved) {
    return postProcessing(saved);
  }

  protected void deletePreProcessing(@NotNull final String id) {
    Optional<Role> roleOptional = repository.findById(id);
    if (roleOptional.isEmpty()) {
      throw new ResourceNotFoundException();
    }
    Role role = roleOptional.get();
    if (!userService.loadUsersByRole(role.getName()).isEmpty()) {
      throw new NotAllowedException("There are Users assigned to this Role");
    }
  }

  protected void deletePostProcessing(@NotNull String id) {}

  @NotNull
  protected Role postProcessing(@NotNull Role entity) {
    return entity;
  }

  protected void updateField(
      @NotNull final Role toUpdate,
      @NotNull final String fieldName,
      @Nullable final Object fieldValue) {
    try {
      @NotNull final Field fieldToUpdate = getField(toUpdate, fieldName);
      fieldToUpdate.setAccessible(true);
      fieldToUpdate.set(toUpdate, fieldValue);
    } catch (NoSuchFieldException e) {
      throw new ResourceUpdateException(
          String.format("Field %s does not exist on this entity!", fieldName), e);
    } catch (IllegalAccessException e) {
      throw new ResourceUpdateException(
          String.format("Field %s can not be updated!", fieldName), e);
    }
  }

  private Field getField(@NotNull final Object obj, @NotNull String fieldName)
      throws NoSuchFieldException {
    Class<?> cls = obj.getClass();
    while (cls != null) {
      try {
        return cls.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        cls = cls.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  protected @NotNull <E extends RoleDto> Role convertDtoToEntity(@NotNull E entity) {
    final Set<Right> rights =
        entity.getRights().stream()
            .map(rightRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toUnmodifiableSet());
    return Role.builder()
        .name(entity.getName())
        .description(entity.getDescription())
        .rights(rights)
        .isProtected(entity.isProtected())
        .build();
  }
}
