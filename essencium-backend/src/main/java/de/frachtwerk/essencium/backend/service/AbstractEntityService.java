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

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Default implementation of an abstract entity service providing basic CRUD functionality
 *
 * @param <OUT> The database entity type
 * @param <IN> The data transfer object passed for creation and update
 */
public abstract class AbstractEntityService<
        OUT extends AbstractBaseModel<ID>, ID extends Serializable, IN>
    extends AbstractCrudService<OUT, ID, IN> {

  protected AbstractEntityService(final @NotNull BaseRepository<OUT, ID> repository) {
    super(repository);
  }

  public AbstractEntityService<OUT, ID, IN> testAccess(@NotNull Specification<OUT> spec) {
    if (existsFiltered(spec)) {
      return this;
    } else {
      throw new ResourceNotFoundException();
    }
  }

  @Override
  protected Specification<OUT> specificationPreProcessing(Specification<OUT> spec) {
    return spec;
  }

  @NotNull
  @Override
  protected List<OUT> getAllPostProcessing(@NotNull List<OUT> allEntities) {
    return allEntities.stream().map(this::postProcessing).toList();
  }

  @NotNull
  @Override
  protected Pageable getAllPreProcessing(@NotNull final Pageable pageable) {
    return pageable;
  }

  @NotNull
  @Override
  protected Page<OUT> getAllPostProcessing(@NotNull final Page<OUT> page) {
    return page.map(this::postProcessing);
  }

  @NotNull
  @Override
  protected ID getByIdPreProcessing(@NotNull final ID id) {
    return id;
  }

  @NotNull
  @Override
  protected OUT getByIdPostProcessing(@NotNull final OUT entity) {
    return postProcessing(entity);
  }

  @NotNull
  @Override
  protected <E extends IN> OUT createPreProcessing(@NotNull final E entity) {
    return convertDtoToEntity(entity);
  }

  @NotNull
  @Override
  protected OUT createPostProcessing(@NotNull final OUT saved) {
    return postProcessing(saved);
  }

  @NotNull
  @Override
  protected <E extends IN> OUT updatePreProcessing(@NotNull final ID id, @NotNull final E entity) {
    final OUT entityToUpdate = convertDtoToEntity(entity);
    if (!Objects.equals(entityToUpdate.getId(), id)) {
      throw new ResourceUpdateException("ID needs to match entity ID");
    }

    Optional<OUT> currentEntityOpt = repository.findById(id);
    if (currentEntityOpt.isEmpty()) {
      throw new ResourceNotFoundException("Entity to update is not persistent");
    }

    entityToUpdate.setCreatedBy(currentEntityOpt.get().getCreatedBy());
    entityToUpdate.setCreatedAt(currentEntityOpt.get().getCreatedAt());
    return entityToUpdate;
  }

  @NotNull
  @Override
  protected OUT updatePostProcessing(@NotNull final OUT saved) {
    return postProcessing(saved);
  }

  @NotNull
  @Override
  protected OUT patchPreProcessing(
      @NotNull final ID id, @NotNull final Map<String, Object> fieldUpdates) {
    OUT out = repository.findById(id).orElseThrow(ResourceNotFoundException::new);
    final var toUpdate = (OUT) out.clone();

    fieldUpdates.remove("createdBy");
    fieldUpdates.remove("createdAt");

    fieldUpdates.forEach((key, value) -> updateField(toUpdate, key, value));
    return toUpdate;
  }

  @NotNull
  @Override
  protected OUT patchPostProcessing(@NotNull final OUT saved) {
    return postProcessing(saved);
  }

  @Override
  protected void deletePreProcessing(@NotNull final ID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException();
    }
  }

  @Override
  protected void deletePostProcessing(@NotNull ID id) {}

  @NotNull
  protected OUT postProcessing(@NotNull OUT entity) {
    return entity;
  }

  @NotNull
  protected abstract <E extends IN> OUT convertDtoToEntity(@NotNull final E entity);

  protected void updateField(
      @NotNull final OUT toUpdate,
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
}
