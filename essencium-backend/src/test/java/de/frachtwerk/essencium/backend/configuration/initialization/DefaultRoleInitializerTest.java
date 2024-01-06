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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.configuration.properties.RoleProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRoleInitializerTest {
  @Mock RoleService roleServiceMock;
  @Mock RoleRepository roleRepositoryMock;
  @Mock RightService rightServiceMock;
  private InitProperties initProperties;
  private List<Right> rights;

  @BeforeEach
  void setUp() {
    initProperties = new InitProperties();
    rights =
        Arrays.stream(BasicApplicationRight.values())
            .map(r -> new Right(r.name(), r.getDescription()))
            .collect(Collectors.toList());
  }

  @Test
  void greenFieldTest() {
    initProperties.setRoles(
        Set.of(
            new RoleProperties(
                "ADMIN",
                "Administrator",
                new HashSet<>(Set.of("USER_READ", "USER_CREATE")),
                true,
                false),
            new RoleProperties("USER", "User", new HashSet<>(Set.of("USER_READ")), false, true)));
    List<Role> roles = new ArrayList<>();
    when(roleServiceMock.getAll()).thenReturn(roles);
    when(roleServiceMock.save(any(Role.class)))
        .thenAnswer(
            invocationOnMock -> {
              Role role = invocationOnMock.getArgument(0);
              roles.add(role);
              return role;
            });
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              Optional<Right> optional =
                  rights.stream().filter(right -> right.getAuthority().equals(authority)).findAny();
              return optional.orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Right with authority [" + authority + "] not found"));
            });

    DefaultRoleInitializer defaultRoleInitializer =
        new DefaultRoleInitializer(
            roleServiceMock, roleRepositoryMock, rightServiceMock, initProperties);
    defaultRoleInitializer.run();

    assertThat(roles).hasSize(2);
    assertThat(roles.stream().map(Role::getAuthority)).contains("ADMIN", "USER");
    assertThat(roles.stream().filter(Role::isDefaultRole)).hasSize(1);
    assertThat(roles.stream().filter(Role::isProtected)).hasSize(1);
    assertThat(
            roles.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().get().getRights())
        .hasSize(BasicApplicationRight.values().length);
    verify(roleServiceMock, times(1)).getAll();
    verify(roleServiceMock, times(2)).save(any(Role.class));
    verifyNoMoreInteractions(roleServiceMock, rightServiceMock);
  }

  @Test
  void brownFieldTest() {
    initProperties.setRoles(
        Set.of(
            new RoleProperties(
                "ADMIN",
                "Administrator",
                new HashSet<>(Set.of("USER_READ", "USER_CREATE")),
                true,
                false),
            new RoleProperties("USER", "User", new HashSet<>(Set.of("USER_READ")), false, true)));
    Set<Role> roles = new HashSet<>();
    when(roleServiceMock.getAll())
        .thenReturn(
            List.of(
                Role.builder()
                    .name("ADMIN")
                    .description("Administrator")
                    .rights(
                        Arrays.stream(BasicApplicationRight.values())
                            .map(r -> new Right(r.name(), r.getDescription()))
                            .collect(Collectors.toSet()))
                    .isProtected(true)
                    .isDefaultRole(false)
                    .isSystemRole(true)
                    .build(),
                Role.builder()
                    .name("USER")
                    .description("User")
                    .rights(Set.of())
                    .isProtected(false)
                    .isDefaultRole(false)
                    .isSystemRole(true)
                    .build()));
    when(roleRepositoryMock.save(any(Role.class)))
        .thenAnswer(
            invocationOnMock -> {
              Role role = invocationOnMock.getArgument(0);
              roles.add(role);
              return role;
            });
    when(rightServiceMock.getByAuthority(anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String authority = invocationOnMock.getArgument(0);
              Optional<Right> optional =
                  rights.stream().filter(right -> right.getAuthority().equals(authority)).findAny();
              return optional.orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Right with authority [" + authority + "] not found"));
            });

    DefaultRoleInitializer defaultRoleInitializer =
        new DefaultRoleInitializer(
            roleServiceMock, roleRepositoryMock, rightServiceMock, initProperties);
    defaultRoleInitializer.run();

    assertThat(roles).hasSize(2);
    assertThat(roles.stream().map(Role::getAuthority)).contains("ADMIN", "USER");
    assertThat(roles.stream().filter(Role::isDefaultRole)).hasSize(1);
    assertThat(roles.stream().filter(Role::isProtected)).hasSize(1);
    assertThat(
            roles.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().get().getRights())
        .hasSize(BasicApplicationRight.values().length);
    verify(roleServiceMock, times(1)).getAll();
    verify(roleRepositoryMock, times(2)).save(any(Role.class));
    verifyNoMoreInteractions(roleServiceMock, roleRepositoryMock, rightServiceMock);
  }
}
