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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.io.Serializable;
import java.util.Optional;

public class UserRepositoryMockConfiguration<I extends Serializable>
    extends BaseRepositoryMockConfiguration<I> {
  public UserRepositoryMockConfiguration(BaseUserRepository<?, I> mockedObject) {
    super(mockedObject);
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenEmailIgnoreCase(
      String email, AbstractBaseUser<?> returnValue) {
    doReturn(Optional.of(returnValue))
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByEmailIgnoreCase(email);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnNoUserForGivenEmailIgnoreCase(String email) {
    doReturn(Optional.empty())
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByEmailIgnoreCase(email);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenPasswordResetToken(
      String token, AbstractBaseUser<?> returnValue) {
    doReturn(Optional.of(returnValue))
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByPasswordResetToken(token);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnNoUserForGivenPasswordResetToken(String token) {
    doReturn(Optional.empty())
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByPasswordResetToken(token);

    return this;
  }

  public UserRepositoryMockConfiguration<I> anotherAdminExistsInTheSystem() {
    doReturn(true)
        .when((BaseUserRepository<?, I>) mockedObject)
        .existsAnyAdminBesidesUserWithId(any(), any());

    return this;
  }
}
