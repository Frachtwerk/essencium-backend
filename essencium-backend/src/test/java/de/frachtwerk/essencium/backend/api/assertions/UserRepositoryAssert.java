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

import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.io.Serializable;

public class UserRepositoryAssert<I extends Serializable> extends RepositoryAssert<I> {
  protected UserRepositoryAssert(BaseRepository<?, I> actual) {
    super(actual);
  }

  public void invokedFindByEmailIgnoreCaseOneTimeFor(String email) {
    verify((BaseUserRepository<?, I>) actual).findByEmailIgnoreCase(email);
  }
}
