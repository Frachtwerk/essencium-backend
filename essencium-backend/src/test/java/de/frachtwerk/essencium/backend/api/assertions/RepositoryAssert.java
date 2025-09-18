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

package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.io.Serializable;
import org.assertj.core.api.AbstractAssert;

public class RepositoryAssert<I extends Serializable>
    extends AbstractAssert<RepositoryAssert<I>, BaseRepository<?, I>> {
  protected RepositoryAssert(BaseRepository<?, I> actual) {
    super(actual, RepositoryAssert.class);
  }

  public void invokedSaveOneTime() {
    invokedSaveNTimes(1);
  }

  public void invokedSaveNTimes(int expectedTimes) {
    verify(actual, times(expectedTimes)).save(any());
  }

  public void invokedFindByIdNTimes(int expectedTimes) {
    verify(actual, times(expectedTimes)).findById(any());
  }

  public void hasNoMoreInteractions() {
    verifyNoMoreInteractions(actual);
  }

  public void invokedDeleteByIdOneTime(I id) {
    verify(actual).deleteById(id);
  }
}
