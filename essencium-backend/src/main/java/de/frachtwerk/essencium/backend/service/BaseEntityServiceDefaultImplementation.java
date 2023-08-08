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
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Default entity service implementation where the DTO for creation and update equals the model
 * itself
 *
 * @param <T> The database entity type
 */
public class BaseEntityServiceDefaultImplementation<
        T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends AbstractEntityService<T, ID, T> {

  BaseEntityServiceDefaultImplementation(final @NotNull BaseRepository<T, ID> repository) {
    super(repository);
  }

  @NotNull
  @Override
  protected <E extends T> T convertDtoToEntity(@NotNull final E entity) {
    return entity;
  }
}
