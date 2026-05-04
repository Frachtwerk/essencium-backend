/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;

class RestExceptionHandlerTest {

  @Test
  void formatValidationErrorUsesFieldArgumentWhenPresent() {
    var fieldArgument = new DefaultMessageSourceResolvable("name");
    var resolvable =
        new DefaultMessageSourceResolvable(
            new String[] {"validation.name"}, new Object[] {fieldArgument}, "must not be blank");

    assertThat(RestExceptionHandler.formatValidationError(resolvable))
        .isEqualTo("name must not be blank");
  }

  @Test
  void formatValidationErrorFallsBackToMessageWhenArgumentsAreEmpty() {
    var resolvable =
        new DefaultMessageSourceResolvable(
            new String[] {"validation.custom"}, new Object[0], "custom validation failed");

    assertThat(RestExceptionHandler.formatValidationError(resolvable))
        .isEqualTo("custom validation failed");
  }

  @Test
  void formatValidationErrorFallsBackToMessageWhenArgumentsAreNull() {
    var resolvable =
        new DefaultMessageSourceResolvable(
            new String[] {"validation.custom"}, null, "custom validation failed");

    assertThat(RestExceptionHandler.formatValidationError(resolvable))
        .isEqualTo("custom validation failed");
  }
}
