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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.repository.UserRepositoryKotlin;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@DisplayName("Interact with the UserService")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock UserRepositoryKotlin userRepositoryMock;

  @InjectMocks private UserServiceKotlin testSubject;

  @Test
  @DisplayName("Fetch all Users as page sorted by custom name parameter")
  void getAllOrderByName() {
    Page<?> page = mock(Page.class);
    Pageable pageableWithSort = PageRequest.of(0, 20, Direction.DESC, "name");

    doReturn(page).when(page).map(any());
    doReturn(page).when(userRepositoryMock).findAll(any(Pageable.class));

    testSubject.getAll(pageableWithSort);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(userRepositoryMock, times(1)).findAll(pageableCaptor.capture());

    Sort expectedSort = Sort.by(Direction.DESC, "firstName", "lastName");
    Pageable capturedPageable = pageableCaptor.getValue();
    Assertions.assertThat(capturedPageable.getSort()).isEqualTo(expectedSort);
  }
}
