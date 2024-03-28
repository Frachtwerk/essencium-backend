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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.configuration.properties.RoleProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRoleInitializerTest {
  @Mock InitProperties initPropertiesMock;
  @Mock RoleRepository roleRepositoryMock;
  @Mock RoleService roleServiceMock;
  @Mock RightService rightServiceMock;

  @InjectMocks DefaultRoleInitializer SUT;

  @AfterEach
  public void verifyMockInteractions() {
    verifyNoMoreInteractions(roleServiceMock, roleRepositoryMock, rightServiceMock);
  }

  @Test
  void order() {
    assertEquals(30, SUT.order());
  }

  @Test
  void getRoleFromProperties() {
    RoleProperties roleProperties = new RoleProperties();
    roleProperties.setName("testRole");
    roleProperties.setDescription("testDescription");
    roleProperties.setProtected(false);
    roleProperties.setDefaultRole(true);
    roleProperties.setRights(Set.of("RIGHT1", "RIGHT2"));

    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              return new Right(authority, authority);
            });

    Role roleFromProperties = SUT.getRoleFromProperties(roleProperties);

    assertEquals("testRole", roleFromProperties.getName());
    assertEquals("testDescription", roleFromProperties.getDescription());
    assertFalse(roleFromProperties.isProtected());
    assertTrue(roleFromProperties.isDefaultRole());
    assertEquals(2, roleFromProperties.getRights().size());
    assertThat(roleFromProperties.getRights())
        .extracting(Right::getAuthority)
        .containsExactlyInAnyOrder("RIGHT1", "RIGHT2");

    verify(rightServiceMock, times(2)).getByAuthority(anyString());
  }

  @Test
  void updateExistingRole() {
    Role oldRole = Role.builder().name("ROLE").build();
    Role newRole =
        Role.builder()
            .name("ROLE")
            .description("newDescription")
            .isProtected(true)
            .isDefaultRole(true)
            .rights(Set.of(new Right("RIGHT1", "RIGHT1"), new Right("RIGHT2", "RIGHT2")))
            .build();

    when(roleRepositoryMock.save(any(Role.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              return new Right(authority, authority);
            });

    SUT.updateExistingRole(newRole, oldRole);

    ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepositoryMock, times(1)).save(roleCaptor.capture());
    verify(rightServiceMock, times(2)).getByAuthority(anyString());
    Role saved = roleCaptor.getValue();

    assertEquals("ROLE", saved.getName());
    assertEquals("newDescription", saved.getDescription());
    assertTrue(saved.isProtected());
    assertTrue(saved.isDefaultRole());
    assertEquals(2, saved.getRights().size());
    assertThat(saved.getRights())
        .extracting(Right::getAuthority)
        .containsExactlyInAnyOrder("RIGHT1", "RIGHT2");
  }

  @Test
  void toManyDefaultRoles() {
    RoleProperties roleProperties = new RoleProperties();
    roleProperties.setName("testRole");
    roleProperties.setDescription("testDescription");
    roleProperties.setProtected(false);
    roleProperties.setDefaultRole(true);
    roleProperties.setRights(new HashSet<>(Set.of("RIGHT1", "RIGHT2")));

    when(initPropertiesMock.getRoles()).thenReturn(new HashSet<>(Set.of(roleProperties)));

    TestRoleInitializer testRoleInitializer =
        new TestRoleInitializer(
            initPropertiesMock, roleRepositoryMock, roleServiceMock, rightServiceMock, true);

    String message =
        assertThrowsExactly(IllegalStateException.class, testRoleInitializer::run).getMessage();

    verify(rightServiceMock, times(BasicApplicationRight.values().length + 2))
        .getByAuthority(anyString());

    assertEquals("More than one role is marked as default role", message);
  }

  @Test
  void missingDefaultRole() {
    when(initPropertiesMock.getRoles()).thenReturn(Set.of());

    String message = assertThrowsExactly(IllegalStateException.class, () -> SUT.run()).getMessage();

    verify(rightServiceMock, times(BasicApplicationRight.values().length))
        .getByAuthority(anyString());

    assertEquals("No role is marked as default role", message);
  }

  @Test
  void greenFieldRun() {
    RoleProperties envRoleProperties = new RoleProperties();
    envRoleProperties.setName("testRole");
    envRoleProperties.setDescription("testDescription");
    envRoleProperties.setProtected(false);
    envRoleProperties.setDefaultRole(true);
    envRoleProperties.setRights(Set.of("RIGHT1", "RIGHT2"));

    TestRoleInitializer testRoleInitializer =
        new TestRoleInitializer(
            initPropertiesMock, roleRepositoryMock, roleServiceMock, rightServiceMock, false);

    when(initPropertiesMock.getRoles()).thenReturn(Set.of(envRoleProperties));
    when(roleServiceMock.getAll()).thenReturn(new ArrayList<>());
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              return new Right(authority, authority);
            });

    testRoleInitializer.run();

    ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepositoryMock, times(4)).save(roleCaptor.capture());
    verify(initPropertiesMock, times(1)).getRoles();
    verify(roleServiceMock, times(1)).getAll();
    verify(
            rightServiceMock,
            times(envRoleProperties.getRights().size() + BasicApplicationRight.values().length))
        .getByAuthority(anyString());
    verify(roleRepositoryMock, times(4)).save(any(Role.class));

    List<Role> allValues = roleCaptor.getAllValues();

    assertThat(allValues)
        .extracting(Role::getName)
        .containsExactlyInAnyOrder("testRole", "TEST", "USER", "ADMIN");
  }

  @Test
  void greenFieldRunNonAdminAdminRole() {
    RoleProperties envRoleProperties = new RoleProperties();
    envRoleProperties.setName("ADMIN");
    envRoleProperties.setDescription("ADMIN");
    envRoleProperties.setProtected(false);
    envRoleProperties.setDefaultRole(true);
    envRoleProperties.setRights(Set.of("RIGHT1", "RIGHT2"));

    when(initPropertiesMock.getRoles()).thenReturn(Set.of(envRoleProperties));
    when(roleServiceMock.getAll()).thenReturn(new ArrayList<>());
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              return new Right(authority, authority);
            });

    SUT.run();

    ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepositoryMock, times(1)).save(roleCaptor.capture());
    verify(initPropertiesMock, times(1)).getRoles();
    verify(roleServiceMock, times(1)).getAll();
    verify(
            rightServiceMock,
            times(envRoleProperties.getRights().size() + BasicApplicationRight.values().length))
        .getByAuthority(anyString());
    verify(roleRepositoryMock, times(1)).save(any(Role.class));

    List<Role> allValues = roleCaptor.getAllValues();
    assertThat(allValues).hasSize(1);
    assertThat(allValues).extracting(Role::getName).containsExactlyInAnyOrder("ADMIN");

    Role role = roleCaptor.getValue();
    assertThat(role.getRights())
        .extracting(Right::getAuthority)
        .containsAll(
            Arrays.stream(BasicApplicationRight.values())
                .map(BasicApplicationRight::getAuthority)
                .toList());
    assertThat(role.getRights()).extracting(Right::getAuthority).contains("RIGHT1", "RIGHT2");
    assertThat(role.getRights()).hasSize(BasicApplicationRight.values().length + 2);
  }

  @Test
  void brownFieldRun() {
    RoleProperties envRoleProperties = new RoleProperties();
    envRoleProperties.setName("testRole");
    envRoleProperties.setDescription("testDescription");
    envRoleProperties.setProtected(false);
    envRoleProperties.setDefaultRole(true);
    envRoleProperties.setRights(Set.of("RIGHT1", "RIGHT2"));

    TestRoleInitializer testRoleInitializer =
        new TestRoleInitializer(
            initPropertiesMock, roleRepositoryMock, roleServiceMock, rightServiceMock, false);

    Role existingRole =
        Role.builder()
            .name("existingRole")
            .description("existingDescription")
            .isProtected(true)
            .isDefaultRole(false)
            .isSystemRole(true)
            .build();

    when(initPropertiesMock.getRoles()).thenReturn(Set.of(envRoleProperties));
    when(roleServiceMock.getAll()).thenReturn(List.of(existingRole));
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              return new Right(authority, authority);
            });

    testRoleInitializer.run();

    ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepositoryMock, times(5)).save(roleCaptor.capture());
    verify(initPropertiesMock, times(1)).getRoles();
    verify(roleServiceMock, times(1)).getAll();
    verify(
            rightServiceMock,
            times(envRoleProperties.getRights().size() + BasicApplicationRight.values().length))
        .getByAuthority(anyString());
    verify(roleRepositoryMock, times(5)).save(any(Role.class));

    List<Role> allValues = roleCaptor.getAllValues();

    assertThat(allValues)
        .extracting(Role::getName)
        .containsExactlyInAnyOrder("testRole", "TEST", "USER", "ADMIN", existingRole.getName());
    assertFalse(existingRole.isSystemRole());
    assertFalse(existingRole.isProtected());
  }
}
