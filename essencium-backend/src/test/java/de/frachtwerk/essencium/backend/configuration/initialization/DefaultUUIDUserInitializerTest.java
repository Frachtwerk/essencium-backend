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

import static de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer.ADMIN_ROLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class DefaultUUIDUserInitializerTest {

  public static final String ADMIN_USERNAME = "admin@frachtwerk.de";
  public static final String ADMIN_FIRST_NAME = "Admin";
  public static final String ADMIN_LAST_NAME = "User";

  private final RoleService roleServiceMock = mock(RoleService.class);
  private final AbstractUserService<TestUUIDUser, UUID, UserDto<UUID>> userServiceMock =
      mock(AbstractUserService.class);

  @BeforeEach
  void setUp() {
    reset(roleServiceMock);
    reset(userServiceMock);
  }

  @Test
  void testAdminAlreadyPersistent() {
    final var sut = new DefaultUserInitializer(roleServiceMock, userServiceMock);

    when(userServiceMock.loadUsersByRole(ADMIN_ROLE_NAME))
        .thenReturn(List.of(mock(TestUUIDUser.class)));

    sut.run();

    verify(userServiceMock, times(1)).loadUsersByRole(ADMIN_ROLE_NAME);
    verifyNoMoreInteractions(userServiceMock);
    verifyNoInteractions(roleServiceMock);
  }

  @Test
  void testNoAdminRolePersistent() {
    final var sut = new DefaultUserInitializer(roleServiceMock, userServiceMock);

    when(userServiceMock.loadUserByUsername(ADMIN_USERNAME))
        .thenThrow(new UsernameNotFoundException(""));

    when(roleServiceMock.getRole(ADMIN_ROLE_NAME)).thenThrow(new ResourceNotFoundException());

    assertThatThrownBy(sut::run).isInstanceOf(IllegalStateException.class);
    verify(userServiceMock, times(1)).loadUsersByRole(ADMIN_ROLE_NAME);
    verify(roleServiceMock, times(1)).getRole(ADMIN_ROLE_NAME);
    verifyNoMoreInteractions(userServiceMock);
    verifyNoMoreInteractions(roleServiceMock);
  }

  @Test
  void testCreateNewAdmin() {
    final var sut = new DefaultUserInitializer(roleServiceMock, userServiceMock);

    when(userServiceMock.loadUserByUsername(ADMIN_USERNAME))
        .thenThrow(new UsernameNotFoundException(""));

    when(userServiceMock.getNewUser()).thenReturn(new UserDto<>());

    var adminRoleMock = mock(Role.class);
    when(roleServiceMock.getRole(ADMIN_ROLE_NAME)).thenReturn(Optional.of(adminRoleMock));

    sut.run();

    verify(userServiceMock, times(1)).loadUsersByRole(ADMIN_ROLE_NAME);
    verify(roleServiceMock, times(1)).getRole(ADMIN_ROLE_NAME);
    var adminUserCapture = ArgumentCaptor.forClass(UserDto.class);
    verify(userServiceMock, times(1)).create(adminUserCapture.capture());

    var adminUserDTO = adminUserCapture.getValue();
    assertThat(adminUserDTO.isEnabled()).isTrue();
    assertThat(adminUserDTO.getEmail()).isEqualTo(ADMIN_USERNAME);
    assertThat(adminUserDTO.getFirstName()).isEqualTo(ADMIN_FIRST_NAME);
    assertThat(adminUserDTO.getLastName()).isEqualTo(ADMIN_LAST_NAME);
    assertThat(adminUserDTO.getRole()).isSameAs(adminRoleMock.getName());
    assertThat(adminUserDTO.getPassword()).isNotBlank();
  }

  @Test
  void testUserHasRoleAuthority() {
    final var rights = List.of(new Right("RIGHT_1", ""), new Right("RIGHT_2", ""));

    final var role = new Role();
    role.setName("ADMIN");
    role.setRights(Set.copyOf(rights));

    final var user = TestUUIDUser.builder().email("test@frachtwerk.de").build();
    user.setRole(role);

    assertThat(
            user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet()))
        .contains(rights.get(0).getAuthority(), rights.get(1).getAuthority(), role.getName());
  }
}
