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

package de.frachtwerk.essencium.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UUIDResetCredentialsControllerTest {

  private final AbstractUserService<TestUUIDUser, UUID, UserDto<UUID>> userServiceMock =
      mock(AbstractUserService.class);

  private final ResetCredentialsController<TestUUIDUser, UUID, UserDto<UUID>> testSubject =
      new ResetCredentialsController(userServiceMock);

  @Test
  void requestResetToken() {
    final var testUsername = "TEST_USERNAME";
    final var testUser = TestUUIDUser.builder().email(testUsername).build();

    when(userServiceMock.loadUserByUsername(testUsername)).thenReturn(testUser);

    doAnswer(
            invocationOnMock -> {
              var passedUsername = invocationOnMock.getArgument(0, String.class);
              assertThat(passedUsername).isEqualTo(testUsername);
              return "";
            })
        .when(userServiceMock)
        .createResetPasswordToken(testUsername);

    testSubject.requestResetToken(testUsername);
  }

  @Test
  void setNewPassword() {
    var testToken = "TEST_USERNAME";
    var newPassword = "NEW_PASSWORD";

    doAnswer(
            invocationOnMock -> {
              var passedToken = invocationOnMock.getArgument(0, String.class);
              var passedPassword = invocationOnMock.getArgument(1, String.class);

              assertThat(passedToken).isEqualTo(testToken);
              assertThat(passedPassword).isEqualTo(newPassword);

              return "";
            })
        .when(userServiceMock)
        .resetPasswordByToken(testToken, newPassword);

    var requestMock = mock(PasswordUpdateRequest.class);

    when(requestMock.verification()).thenReturn(testToken);
    when(requestMock.password()).thenReturn(newPassword);

    testSubject.setNewPassword(requestMock);
  }
}
