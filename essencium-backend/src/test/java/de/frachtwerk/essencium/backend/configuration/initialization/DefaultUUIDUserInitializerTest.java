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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.user.TestUUIDUser;
import de.frachtwerk.essencium.backend.configuration.properties.EssenciumInitProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultUUIDUserInitializerTest {
  @Mock
  AbstractUserService<TestUUIDUser, EssenciumUserDetails<UUID>, UUID, BaseUserDto<UUID>>
      userServiceMock;

  private EssenciumInitProperties essenciumInitProperties;

  @BeforeEach
  void setUp() {
    essenciumInitProperties = new EssenciumInitProperties();
  }

  @Test
  void greenFieldTest() {
    essenciumInitProperties.setUsers(
        Set.of(
            Map.of(
                "username",
                "devnull@frachtwerk.de",
                "password",
                "adminAdminAdmin",
                "firstName",
                "Admin",
                "lastName",
                "User",
                "roles",
                List.of("ADMIN")),
            Map.of(
                "username",
                "user@frachtwerk.de",
                "password",
                "userUserUser",
                "first-name",
                "User",
                "last-Name",
                "User",
                "roles",
                List.of("USER"))));
    List<TestUUIDUser> userDB = new ArrayList<>();

    when(userServiceMock.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(userServiceMock.create(any(BaseUserDto.class)))
        .thenAnswer(
            invocation -> {
              BaseUserDto<UUID> entity = invocation.getArgument(0);
              TestUUIDUser user =
                  TestUUIDUser.builder()
                      .email(entity.getEmail())
                      .enabled(entity.isEnabled())
                      .roles(
                          entity.getRoles().stream()
                              .map(s -> Role.builder().name(s).build())
                              .collect(Collectors.toCollection(HashSet::new)))
                      .firstName(entity.getFirstName())
                      .lastName(entity.getLastName())
                      .locale(entity.getLocale())
                      .source(entity.getSource())
                      .build();
              user.setId(UUID.randomUUID());
              userDB.add(user);
              return user;
            });
    when(userServiceMock.getNewUser()).thenReturn(new BaseUserDto<>());

    DefaultUserInitializer<TestUUIDUser, EssenciumUserDetails<UUID>, BaseUserDto<UUID>, UUID> SUT =
        new DefaultUserInitializer<>(userServiceMock, essenciumInitProperties);
    SUT.run();

    assertThat(userDB).hasSize(2);
    assertThat(userDB.stream().map(AbstractBaseUser::getEmail))
        .contains("devnull@frachtwerk.de", "user@frachtwerk.de");

    verify(userServiceMock, times(2)).findByEmailIgnoreCase(anyString());
    verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void brownFieldTest() {
    essenciumInitProperties.setUsers(
        Set.of(
            Map.of(
                "username",
                "devnull@frachtwerk.de",
                "password",
                "adminAdminAdmin",
                "firstName",
                "Admin",
                "lastName",
                "User",
                "roles",
                List.of("ADMIN")),
            Map.of(
                "username",
                "user@frachtwerk.de",
                "password",
                "userUserUser",
                "first-name",
                "User",
                "last-Name",
                "User",
                "roles",
                List.of("USER"))));
    List<TestUUIDUser> userDB = new ArrayList<>();
    userDB.add(
        TestUUIDUser.builder()
            .email("devnull@frachtwerk.de")
            .enabled(true)
            .roles(new HashSet<>(Set.of(Role.builder().name("ADMIN").build())))
            .firstName("Admin")
            .lastName("User")
            .locale(Locale.GERMAN)
            .source("test")
            .id(UUID.randomUUID())
            .build());

    when(userServiceMock.findByEmailIgnoreCase("devnull@frachtwerk.de"))
        .thenReturn(Optional.of(userDB.getFirst()));
    when(userServiceMock.create(any(BaseUserDto.class)))
        .thenAnswer(
            invocation -> {
              BaseUserDto<UUID> entity = invocation.getArgument(0);
              TestUUIDUser user =
                  TestUUIDUser.builder()
                      .email(entity.getEmail())
                      .enabled(entity.isEnabled())
                      .roles(
                          entity.getRoles().stream()
                              .map(s -> Role.builder().name(s).build())
                              .collect(Collectors.toCollection(HashSet::new)))
                      .firstName(entity.getFirstName())
                      .lastName(entity.getLastName())
                      .locale(entity.getLocale())
                      .source(entity.getSource())
                      .build();
              user.setId(UUID.randomUUID());
              userDB.add(user);
              return user;
            });
    when(userServiceMock.getNewUser()).thenReturn(new BaseUserDto<>());

    DefaultUserInitializer<TestUUIDUser, EssenciumUserDetails<UUID>, BaseUserDto<UUID>, UUID> SUT =
        new DefaultUserInitializer<>(userServiceMock, essenciumInitProperties);
    SUT.run();

    assertThat(userDB).hasSize(2);
    assertThat(userDB.stream().map(AbstractBaseUser::getEmail))
        .contains("devnull@frachtwerk.de", "user@frachtwerk.de");

    verify(userServiceMock, times(2)).findByEmailIgnoreCase(anyString());
    verify(userServiceMock, times(1)).getNewUser();
    verify(userServiceMock, times(1)).create(any(BaseUserDto.class));
    verify(userServiceMock, times(1)).patch(any(UUID.class), anyMap());

    verifyNoMoreInteractions(userServiceMock);
  }
}
