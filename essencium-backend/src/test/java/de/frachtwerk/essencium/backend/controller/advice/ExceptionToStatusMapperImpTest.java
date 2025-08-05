/*
 *
 *  * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *  *
 *  * This file is part of essencium-backend.
 *  *
 *  * essencium-backend is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * essencium-backend is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.frachtwerk.essencium.backend.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.exception.EssenciumException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ExceptionToStatusMapperImpTest {

  private ExceptionToStatusMapperImp sut;

  @BeforeEach
  void setUp() {
    this.sut = new ExceptionToStatusMapperImp();
  }

  @Test
  @DisplayName("Map correct EssenciumException")
  void testMapCorrect() {
    assertThat(sut.map(new EssenciumException("Exception")))
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("Map hierarchically correct non existing in mapping Exception")
  void testMapHierarchy() {
    assertThat(sut.map(new StrictlyNotAllowedException("Exception")))
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Map non existing Exception to inter server error")
  void testMapNonExistingException() {
    sut.exceptionMap.clear();
    assertThat(sut.map(new RuntimeException())).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static class StrictlyNotAllowedException extends NotAllowedException {

    public StrictlyNotAllowedException(String message) {
      super(message);
    }

    public StrictlyNotAllowedException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
