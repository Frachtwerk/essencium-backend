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

package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.io.Serializable;
import java.util.Optional;

public class BaseRepositoryMockConfiguration<I extends Serializable> implements MockConfiguration {

  protected final BaseRepository<?, I> mockedObject;

  public BaseRepositoryMockConfiguration(BaseRepository<?, I> mockedObject) {
    this.mockedObject = mockedObject;
  }

  public BaseRepositoryMockConfiguration<I> returnAlwaysPassedObjectOnSave() {
    doAnswer(returnsFirstArg()).when(mockedObject).save(any());
    return this;
  }

  public BaseRepositoryMockConfiguration<I> returnOnFindByIdFor(I id, Object returnValue) {
    doReturn(Optional.of(returnValue)).when(mockedObject).findById(id);

    return this;
  }

  public BaseRepositoryMockConfiguration<I> entityWithIdExists(I id) {
    doReturn(true).when(mockedObject).existsById(id);

    return this;
  }

  public BaseRepositoryMockConfiguration<I> doNothingOnDeleteEntityWithId(I id) {
    doNothing().when(mockedObject).deleteById(id);

    return this;
  }
}
