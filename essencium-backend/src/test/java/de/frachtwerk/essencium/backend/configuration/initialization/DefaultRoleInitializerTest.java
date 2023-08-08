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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.DefaultRoleProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DefaultRoleInitializerTest {

  private final RightService rightServiceMock = mock(RightService.class);
  private final RoleService roleServiceMock = mock(RoleService.class);
  private final DefaultRoleProperties defaultRoleProperties = mock(DefaultRoleProperties.class);

  private final DefaultRoleInitializer testSubject =
      new DefaultRoleInitializer(rightServiceMock, roleServiceMock, defaultRoleProperties);

  @BeforeEach
  void setUp() {
    reset(rightServiceMock);
    reset(roleServiceMock);
    when(defaultRoleProperties.getName()).thenReturn("USER");
    when(defaultRoleProperties.getDescription()).thenReturn("Application user");
  }

  @Test
  void testGetAdminRights() {
    final var testAdminRights = List.of(new Right("TEST_RIGHT", "Test description"));
    when(rightServiceMock.getAll()).thenReturn(testAdminRights);

    final var testResult = testSubject.getAdminRights();

    assertThat(testResult).containsExactlyElementsOf(testAdminRights);
    verify(rightServiceMock, times(1)).getAll();
  }

  @Test
  void testGetUserRights() {
    final var testResult = testSubject.getUserRights();

    assertThat(testResult).isEmpty();
    verifyNoInteractions(rightServiceMock);
  }

  @Test
  void testGetAdminRole() {
    final var testAdminRights = List.of(mock(Right.class));
    final Set<String> testAdminRightsIds =
        testAdminRights.stream().map(Right::getAuthority).collect(Collectors.toSet());
    when(rightServiceMock.getAll()).thenReturn(testAdminRights);

    final var testResult = testSubject.getAdminRole();

    assertThat(testResult.getName()).isEqualTo(DefaultRoleInitializer.ADMIN_ROLE_NAME);
    assertThat(testResult.getDescription())
        .isEqualTo(DefaultRoleInitializer.ADMIN_ROLE_DESCRIPTION);
    assertThat(testResult.getRights()).containsExactlyElementsOf(testAdminRightsIds);
  }

  @Test
  void testGetUserRole() {
    final var testResult = testSubject.getUserRole();

    assertThat(testResult.getName()).isEqualTo(defaultRoleProperties.getName());
    assertThat(testResult.getDescription()).isEqualTo(defaultRoleProperties.getDescription());
    assertThat(testResult.getRights()).isEmpty();
  }

  @Test
  void testNonExistingRoles() {
    var testAdminRights =
        List.of(
            Right.builder().authority("TEST_RIGHT").build(),
            Right.builder().authority("TEST_RIGHT2").build());

    List<String> testAdminRightsIds = new ArrayList<>();
    testAdminRights.forEach(right -> testAdminRightsIds.add(right.getAuthority()));
    when(rightServiceMock.getAll()).thenReturn(testAdminRights);
    when(roleServiceMock.create(any(RoleDto.class))).thenReturn(null);

    testSubject.run();

    final var captor = ArgumentCaptor.forClass(RoleDto.class);
    verify(roleServiceMock, times(2)).create(captor.capture());

    final var createdRoles = captor.getAllValues();

    assertThat(createdRoles).hasSize(2);
    assertThat(createdRoles.get(0).getName()).isEqualTo(DefaultRoleInitializer.ADMIN_ROLE_NAME);
    assertThat(createdRoles.get(0).getDescription())
        .isEqualTo(DefaultRoleInitializer.ADMIN_ROLE_DESCRIPTION);
    assertThat(createdRoles.get(0).getRights())
        .containsExactlyInAnyOrderElementsOf(testAdminRightsIds);
    assertThat(createdRoles.get(0).isProtected()).isTrue();

    assertThat(createdRoles.get(1).getName()).isEqualTo(defaultRoleProperties.getName());
    assertThat(createdRoles.get(1).getDescription())
        .isEqualTo(defaultRoleProperties.getDescription());
    assertThat(createdRoles.get(1).getRights()).isEmpty();
    assertThat(createdRoles.get(1).isProtected()).isFalse();
  }

  @Test
  void testExistingRoles() {
    // Existing protected roles must get overwritten by what it is defined in code.
    // Existing non-protected roles, however, remain untouched. Changes made by a user during
    // runtime are being kept. However, changes made by a developer programatically are kept,
    // too. To explicitly update the properties (e.g. set of rights after introducing a new right)
    // of a role in an existing database in use, you – as a developer – would need to write
    // custom initializer logic.

    final var EXISTING_ADMIN_ROLE_NAME = "ADMIN";
    final var EXISTING_ADMIN_ROLE_DESC = "Admin after role was edited";
    final var EXISTING_USER_ROLE_NAME = "MODIFIED_USER";
    final var EXISTING_USER_ROLE_DESC = "User after role was edited";

    final var testRights = List.of(new Right("r1", ""), new Right("r2", ""));

    final var existingAdminRole =
        Role.builder()
            .name(EXISTING_ADMIN_ROLE_NAME)
            .description(EXISTING_ADMIN_ROLE_DESC)
            .rights(Set.of()) // all rights by default, no rights now
            .build();

    final var existingUserRole =
        Role.builder()
            .name(EXISTING_USER_ROLE_NAME)
            .description(EXISTING_USER_ROLE_DESC)
            .rights(Set.of(testRights.get(0))) // no rights by default, one right now
            .build();

    when(rightServiceMock.getAll()).thenReturn(testRights);
    when(roleServiceMock.getAll()).thenReturn((List.of(existingAdminRole, existingUserRole)));
    when(roleServiceMock.update(anyString(), any(RoleDto.class))).thenReturn(null);
    when(roleServiceMock.create(any(RoleDto.class))).thenReturn(null);

    testSubject.run();

    verify(roleServiceMock).getAll();

    final var roleUpdateCaptor = ArgumentCaptor.forClass(RoleDto.class);
    verify(roleServiceMock).update(eq("ADMIN"), roleUpdateCaptor.capture());

    final var updatedRoles = roleUpdateCaptor.getAllValues();
    assertThat(updatedRoles).hasSize(1);
    assertThat(updatedRoles.get(0))
        .hasFieldOrPropertyWithValue("name", DefaultRoleInitializer.ADMIN_ROLE_NAME);
    assertThat(updatedRoles.get(0))
        .hasFieldOrPropertyWithValue("description", DefaultRoleInitializer.ADMIN_ROLE_DESCRIPTION);
    assertThat(updatedRoles.get(0)).hasFieldOrPropertyWithValue("isProtected", true);
    assertThat(updatedRoles.get(0).getRights()).isNotEmpty();

    final var roleCreateCaptor = ArgumentCaptor.forClass(RoleDto.class);
    verify(roleServiceMock).create(roleCreateCaptor.capture());

    final var createdRoles = roleCreateCaptor.getAllValues();
    assertThat(createdRoles).hasSize(1);
    assertThat(createdRoles.get(0))
        .hasFieldOrPropertyWithValue("name", defaultRoleProperties.getName());
    assertThat(createdRoles.get(0))
        .hasFieldOrPropertyWithValue("description", defaultRoleProperties.getDescription());
    assertThat(createdRoles.get(0)).hasFieldOrPropertyWithValue("isProtected", false);
    assertThat(createdRoles.get(0).getRights()).isEmpty();

    // no more updates, especially not to unprotected user role
    verifyNoMoreInteractions(roleServiceMock);
  }

  @Test
  void testNoActionOnIdenticalRoles() {

    final var EXISTING_ADMIN_ROLE_NAME = DefaultRoleInitializer.ADMIN_ROLE_NAME;
    final var EXISTING_ADMIN_ROLE_DESC = DefaultRoleInitializer.ADMIN_ROLE_DESCRIPTION;
    final var EXISTING_USER_ROLE_NAME = defaultRoleProperties.getName();
    final var EXISTING_USER_ROLE_DESC = defaultRoleProperties.getDescription();

    Collection<Right> testRights = testSubject.getAdminRights();

    final var existingAdminRole =
        Role.builder()
            .name(EXISTING_ADMIN_ROLE_NAME)
            .description(EXISTING_ADMIN_ROLE_DESC)
            .rights(new HashSet<>(testRights)) // all rights by default, no rights now
            .build();

    final var existingUserRole =
        Role.builder()
            .name(EXISTING_USER_ROLE_NAME)
            .description(EXISTING_USER_ROLE_DESC)
            .rights(Set.of()) // no rights by default, one right now
            .build();

    when(rightServiceMock.getAll()).thenReturn(testRights.stream().toList());
    when(roleServiceMock.getAll()).thenReturn(List.of(existingAdminRole, existingUserRole));

    testSubject.run();

    verify(roleServiceMock).getAll();
    verifyNoMoreInteractions(roleServiceMock);
  }
}
