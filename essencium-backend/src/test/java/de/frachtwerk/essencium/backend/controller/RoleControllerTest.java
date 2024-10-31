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

package de.frachtwerk.essencium.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class RoleControllerTest {

  private final RoleService roleServiceMock = Mockito.mock(RoleService.class);

  private final RoleController testSubject = new RoleController(roleServiceMock);

  @Test
  @SuppressWarnings("unchecked")
  void findAll() {
    var testPageable = Mockito.mock(Pageable.class);
    var userPageMock = Mockito.mock(Page.class);

    Mockito.when(roleServiceMock.getAll(testPageable)).thenReturn(userPageMock);

    assertThat(testSubject.findAll(testPageable)).isSameAs(userPageMock);

    Mockito.verify(roleServiceMock).getAll(testPageable);
  }

  @Test
  void findById() {
    String testName = "42L";
    Role roleMock = Mockito.mock(Role.class);

    Mockito.when(roleServiceMock.getByName(testName)).thenReturn(roleMock);

    assertThat(testSubject.findById(testName)).isSameAs(roleMock);

    Mockito.verify(roleServiceMock).getByName(testName);
  }

  @Test
  void create() {
    var testCreationRole = Mockito.mock(RoleDto.class);
    Role createdRoleMock = Mockito.mock(Role.class);

    Mockito.when(roleServiceMock.create(testCreationRole)).thenReturn(createdRoleMock);

    assertThat(testSubject.create(testCreationRole)).isSameAs(createdRoleMock);

    Mockito.verify(roleServiceMock).create(testCreationRole);
  }

  @Test
  void update() {
    var testId = "TEST_ROLE_ABC";
    var testUpdateRole = Mockito.mock(RoleDto.class);
    Role updatedRoleMock = Mockito.mock(Role.class);

    Mockito.when(testUpdateRole.getName()).thenReturn(testId);
    Mockito.when(updatedRoleMock.getName()).thenReturn(testId);
    Mockito.when(roleServiceMock.update(testId, testUpdateRole)).thenReturn(updatedRoleMock);

    assertThat(testSubject.update(testId, testUpdateRole)).isSameAs(updatedRoleMock);

    Mockito.verify(roleServiceMock).update(testId, testUpdateRole);
  }

  @Test
  @SuppressWarnings("unchecked")
  void patch() {
    var testId = "42L";
    var testRoleMap = Mockito.mock(Map.class);
    Role updatedRoleMock = Mockito.mock(Role.class);

    Mockito.when(roleServiceMock.patch(testId, testRoleMap)).thenReturn(updatedRoleMock);

    assertThat(testSubject.patch(testId, testRoleMap)).isSameAs(updatedRoleMock);

    Mockito.verify(roleServiceMock).patch(testId, testRoleMap);
  }

  @Test
  void delete() {
    var testId = "42L";

    testSubject.delete(testId);
    Mockito.verify(roleServiceMock).deleteById(testId);
  }
}
