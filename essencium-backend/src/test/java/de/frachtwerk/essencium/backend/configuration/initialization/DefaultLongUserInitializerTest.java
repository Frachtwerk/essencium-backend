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

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.configuration.properties.EssenciumInitProperties;
import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.util.*;
import java.util.stream.Collectors;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultLongUserInitializerTest {
  @Mock
  AbstractUserService<UserStub, EssenciumUserDetails<Long>, Long, BaseUserDto<Long>>
      userServiceMock;

  private EssenciumInitProperties essenciumInitProperties;
  private Random random;

  @BeforeEach
  void setUp() {
    essenciumInitProperties = new EssenciumInitProperties();
    random = new Random();
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
    List<UserStub> userDB = new ArrayList<>();

    when(userServiceMock.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(userServiceMock.create(any(BaseUserDto.class)))
        .thenAnswer(
            invocation -> {
              BaseUserDto<UUID> entity = invocation.getArgument(0);
              UserStub user =
                  UserStub.builder()
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
              user.setId(random.nextLong());
              userDB.add(user);
              return user;
            });
    when(userServiceMock.getNewUser()).thenReturn(new BaseUserDto<>());

    DefaultUserInitializer<UserStub, EssenciumUserDetails<Long>, BaseUserDto<Long>, Long> sut =
        new DefaultUserInitializer<>(userServiceMock, essenciumInitProperties);

    sut.run();

    AssertionsForInterfaceTypes.assertThat(userDB).hasSize(2);
    AssertionsForInterfaceTypes.assertThat(userDB.stream().map(AbstractBaseUser::getEmail))
        .contains("devnull@frachtwerk.de", "user@frachtwerk.de");

    verify(userServiceMock, times(2)).findByEmailIgnoreCase(anyString());
    verify(userServiceMock, times(2)).getNewUser();
    verify(userServiceMock, times(2)).create(any(BaseUserDto.class));

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
    List<UserStub> userDB = new ArrayList<>();
    userDB.add(
        UserStub.builder()
            .email("devnull@frachtwerk.de")
            .enabled(true)
            .roles(new HashSet<>(Set.of(Role.builder().name("ADMIN").build())))
            .firstName("Admin")
            .lastName("User")
            .locale(Locale.GERMAN)
            .source("test")
            .id(random.nextLong())
            .build());

    when(userServiceMock.findByEmailIgnoreCase("devnull@frachtwerk.de"))
        .thenReturn(Optional.of(userDB.getFirst()));
    when(userServiceMock.findByEmailIgnoreCase("user@frachtwerk.de")).thenReturn(Optional.empty());
    when(userServiceMock.create(any(BaseUserDto.class)))
        .thenAnswer(
            invocation -> {
              BaseUserDto<UUID> entity = invocation.getArgument(0);
              UserStub user =
                  UserStub.builder()
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
              user.setId(random.nextLong());
              userDB.add(user);
              return user;
            });
    when(userServiceMock.getNewUser()).thenReturn(new BaseUserDto<>());

    DefaultUserInitializer<UserStub, EssenciumUserDetails<Long>, BaseUserDto<Long>, Long> sut =
        new DefaultUserInitializer<>(userServiceMock, essenciumInitProperties);
    sut.run();

    AssertionsForInterfaceTypes.assertThat(userDB).hasSize(2);
    AssertionsForInterfaceTypes.assertThat(userDB.stream().map(AbstractBaseUser::getEmail))
        .contains("devnull@frachtwerk.de", "user@frachtwerk.de");

    verify(userServiceMock, times(2)).findByEmailIgnoreCase(anyString());
    verify(userServiceMock, times(1)).getNewUser();
    verify(userServiceMock, times(1)).create(any(BaseUserDto.class));
    verify(userServiceMock, times(1)).patch(anyLong(), anyMap());

    verifyNoMoreInteractions(userServiceMock);
  }
}
