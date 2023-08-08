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
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Abstract entity service providing basic CRUD functionality
 *
 * @param <T> The database entity type
 * @param <DTO> The data transfer object passed for creation and update
 */
public abstract class AbstractCrudService<
    T extends AbstractBaseModel<ID>, ID extends Serializable, DTO> {

  protected final BaseRepository<T, ID> repository;

  protected AbstractCrudService(@NotNull final BaseRepository<T, ID> repository) {
    this.repository = repository;
  }

  /**
   * Returns the count of all persistent entities of type T
   *
   * @return the entity count
   */
  public final long countAll() {
    return repository.count();
  }

  public final long countFiltered(Specification<T> specification) {
    final Specification<T> spec = specificationPreProcessing(specification);
    return repository.count(spec);
  }

  public final boolean existsById(@NotNull final ID id) {
    return repository.existsById(id);
  }

  public final boolean existsFiltered(Specification<T> specification) {
    final Specification<T> spec = specificationPreProcessing(specification);
    return repository.exists(spec);
  }

  /**
   * Returns all entities of type T that are stored in the database.
   *
   * @return list of all entities of type T within the database
   */
  @NotNull
  public final List<T> getAll() {
    final var allEntities = repository.findAll();

    return getAllPostProcessing(allEntities);
  }

  /**
   * Returns all entities of type T that correspond to a certain JPA specification.
   *
   * @param specification specification describing filters to apply to the set of all entities
   * @return list of all entities matching the specification
   */
  @NotNull
  public final List<T> getAllFiltered(Specification<T> specification) {
    final Specification<T> spec = specificationPreProcessing(specification);
    return getAllPostProcessing(repository.findAll(spec));
  }

  /**
   * Returns all entities of type T that correspond to a certain JPA specification, limited and
   * sorted by the provided pageable request.
   *
   * @param specification specification describing filters to apply to the set of all entities
   * @param pageable the pageable request limiting the returned list
   * @return list of all entities matching the specification with limitations and sorting according
   *     to the provided pageable request
   */
  @NotNull
  public final Page<T> getAllFiltered(Specification<T> specification, Pageable pageable) {
    final Specification<T> spec = specificationPreProcessing(specification);
    final var processedPageable = getAllPreProcessing(pageable);
    return getAllPostProcessing(repository.findAll(spec, processedPageable));
  }

  /**
   * Returns all entities of the database limited and sorted by the provided pageable request.
   *
   * @param pageable the pageable request limiting the returned list
   * @return page of elements with limitations and sorting according to the provided pageable
   *     request
   */
  @NotNull
  public final Page<T> getAll(@NotNull final Pageable pageable) {
    final var processedPageable = getAllPreProcessing(pageable);

    final var page = repository.findAll(processedPageable);

    return getAllPostProcessing(page);
  }

  @NotNull
  public final Optional<T> getOne(Specification<T> specification) {
    final Specification<T> spec = specificationPreProcessing(specification);
    return repository.findOne(spec).map(this::getByIdPostProcessing);
  }

  /**
   * Get single entity by its database ID
   *
   * @param id the database id of the requested entity
   * @return the requested entity
   * @throws ResourceNotFoundException if an entity with the requested id is not present in the
   *     database
   */
  @NotNull
  public final T getById(@NotNull final ID id) {
    final var processedId = getByIdPreProcessing(id);

    final var entity = repository.findById(processedId).orElseThrow(ResourceNotFoundException::new);

    return getByIdPostProcessing(entity);
  }

  /**
   * Creates a new entity in the database based on the provided entity representation
   *
   * @param entity the entity representation used to create a new database entity
   * @param <E> type of the provided entity representation data transfer object. The DTO-type is in
   *     most cases equal to the model type. If not the createPreProcessing() function has to be
   *     implemented by the inheriting class.
   * @return the saved entity as persisted in the database
   */
  @NotNull
  public final <E extends DTO> T create(@NotNull final E entity) {
    final var processedEntity = createPreProcessing(entity);

    final var saved = repository.save(processedEntity);

    return createPostProcessing(saved);
  }

  /**
   * Updates an already existing entity in the database based on the provided entity representation.
   *
   * @param id the id of the entity that shall be updated
   * @param entity the entity representation used to update an existing database entity
   * @param <E> type of the provided entity representation data transfer object. The DTO-type is in
   *     most cases equal to the model type. If not the updatePreProcessing() function has to be
   *     implemented by the inheriting class.
   * @return the saved entity as persisted in the database
   * @throws ResourceUpdateException if the ID does not match the representation ID
   * @throws ResourceNotFoundException if an entity with the requested id is not present in the
   *     database
   */
  @NotNull
  public final <E extends DTO> T update(@NotNull final ID id, @NotNull final E entity) {
    final var processedEntity = updatePreProcessing(id, entity);

    final var saved = repository.save(processedEntity);

    return updatePostProcessing(saved);
  }

  /**
   * Patches an already existing entity in the database based on the provided value map
   *
   * @param id the id of the entity that shall be updated
   * @param fieldUpdates map with the new values for the entity fields. the value-keys has to map
   *     the field names of the entity model class.
   * @return the saved entity as persisted in the database
   * @throws ResourceNotFoundException if an entity with the requested id is not present in the
   *     database
   * @throws ResourceUpdateException if the maps contains a field that can not be mapped to a field
   *     of the entity model class or this field can not be accessed.
   */
  @NotNull
  public final T patch(@NotNull final ID id, @NotNull final Map<String, Object> fieldUpdates) {
    final var toUpdate = patchPreProcessing(id, fieldUpdates);

    final var saved = repository.save(toUpdate);

    return patchPostProcessing(saved);
  }

  /**
   * Deletes an existing entity from the database.
   *
   * @param id the id of the entity to be deleted
   * @throws ResourceNotFoundException if an entity with the requested id is not present in the
   *     database
   */
  public final void deleteById(@NotNull final ID id) {
    deletePreProcessing(id);
    repository.deleteById(id);
    deletePostProcessing(id);
  }

  protected abstract Specification<T> specificationPreProcessing(Specification<T> spec);

  /**
   * Post-processing function that is called after a getAll request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param allEntities list with all entities stored within the database
   * @return the list that shall be presented after the getAll call.
   */
  @NotNull
  protected abstract List<T> getAllPostProcessing(@NotNull final List<T> allEntities);

  /**
   * Pre-processing function that is called before a getAll request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param pageable the pageable request passed to the getAll method.
   * @return a pageable object that shall be presented to the database repository
   */
  @NotNull
  protected abstract Pageable getAllPreProcessing(@NotNull final Pageable pageable);

  /**
   * Post-processing function that is called after a getAll request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param page the page that was returned by the database repository
   * @return the page that shall be presented after the getAll call.
   */
  @NotNull
  protected abstract Page<T> getAllPostProcessing(@NotNull final Page<T> page);

  /**
   * Pre-processing function that is called before a getById request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param id the id passed to the getById method.
   * @return an id that shall be presented to the database repository
   */
  @NotNull
  protected abstract ID getByIdPreProcessing(@NotNull final ID id);

  /**
   * Post-processing function that is called after a getById request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param entity the entity that was returned by the database repository
   * @return the entity that shall be presented after the getById call.
   */
  @NotNull
  protected abstract T getByIdPostProcessing(@NotNull final T entity);

  /**
   * Pre-processing function that is called before a create request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param entity the entity representation passed to the create method.
   * @return an entity that shall be saved by the database repository
   */
  @NotNull
  protected abstract <E extends DTO> T createPreProcessing(@NotNull final E entity);

  /**
   * Post-processing function that is called after a create request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param saved the entity that was saved by the database repository
   * @return the entity that shall be presented after the create call.
   */
  @NotNull
  protected abstract T createPostProcessing(@NotNull final T saved);

  /**
   * Pre-processing function that is called before an update request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param id the id of the database entity to update passed to the update method.
   * @param entity the entity representation passed to the update method.
   * @return an entity that shall be saved by the database repository
   */
  @NotNull
  protected abstract <E extends DTO> T updatePreProcessing(
      @NotNull final ID id, @NotNull final E entity);

  /**
   * Post-processing function that is called after an update request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param saved the entity that was saved by the database repository
   * @return the entity that shall be presented after the update call.
   */
  @NotNull
  protected abstract T updatePostProcessing(@NotNull final T saved);

  /**
   * Pre-processing function that is called before a patch request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param id the id of the database entity to patch passed to the patch method.
   * @param fieldUpdates field map that shall be patched on the database entity
   * @return an entity that shall be saved by the database repository
   */
  @NotNull
  protected abstract T patchPreProcessing(
      @NotNull final ID id, @NotNull final Map<String, Object> fieldUpdates);

  /**
   * Post-processing function that is called after a patch request.
   *
   * <p>This function might be used for output validation or manipulation.
   *
   * @param saved the entity that was saved by the database repository
   * @return the entity that shall be presented after the patch call.
   */
  @NotNull
  protected abstract T patchPostProcessing(@NotNull final T saved);

  /**
   * Pre-processing function that is called before a delete request.
   *
   * <p>This function might be used for input validation or manipulation.
   *
   * @param id the id of the database entity to delete passed to the deleteById method.
   */
  protected abstract void deletePreProcessing(@NotNull final ID id);

  protected abstract void deletePostProcessing(@NotNull final ID id);
}
