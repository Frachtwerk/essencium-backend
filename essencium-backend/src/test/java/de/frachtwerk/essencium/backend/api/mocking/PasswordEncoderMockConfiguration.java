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

import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderMockConfiguration implements MockConfiguration {

  private final PasswordEncoder mockedObject;

  public PasswordEncoderMockConfiguration(PasswordEncoder mockedObject) {
    this.mockedObject = mockedObject;
  }

  public PasswordEncoderMockConfiguration writePassedPasswordInAndReturn(
      AtomicReference<String> capturedPassword, String encodedPassword) {
    doAnswer(
            invocation -> {
              capturedPassword.set(invocation.getArgument(0));
              return encodedPassword;
            })
        .when(mockedObject)
        .encode(anyString());
    return this;
  }

  public PasswordEncoderMockConfiguration returnEncodedPasswordWhenPasswordGiven(
      String encodedPassword, String password) {
    doReturn(encodedPassword).when(mockedObject).encode(password);

    return this;
  }

  public PasswordEncoderMockConfiguration passGivenPassword(
      CharSequence password, String passwordHash) {
    doReturn(true).when(mockedObject).matches(password, passwordHash);

    return this;
  }
}
