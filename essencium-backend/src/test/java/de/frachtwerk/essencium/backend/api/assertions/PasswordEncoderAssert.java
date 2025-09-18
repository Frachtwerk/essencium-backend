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

package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.AbstractAssert;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderAssert extends AbstractAssert<PasswordEncoderAssert, PasswordEncoder> {

  protected PasswordEncoderAssert(PasswordEncoder actual) {
    super(actual, PasswordEncoderAssert.class);
  }

  public void passwordUpdateMethodsAreTriggeredOnes() {
    verify(actual).matches(anyString(), anyString());
    verify(actual).encode(anyString());
  }
}
