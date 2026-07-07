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

package de.frachtwerk.essencium.backend.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseTest {

  private static final Map<String, Object> ERROR_ATTRIBUTES =
      new HashMap<>(
          Map.of(
              "error", "Bad Request",
              "message", "validation failed",
              "path", "/test/path",
              "timestamp", LocalDateTime.of(2026, 1, 1, 0, 0),
              "status", 400));

  @Test
  void messageIsIncludedWhenFlagIsTrue() {
    ErrorResponse response = new ErrorResponse(400, ERROR_ATTRIBUTES, true);

    assertThat(response.getMessage()).isEqualTo("validation failed");
  }

  @Test
  void messageIsNullWhenFlagIsFalse() {
    ErrorResponse response = new ErrorResponse(400, ERROR_ATTRIBUTES, false);

    assertThat(response.getMessage()).isNull();
  }

  @Test
  void messageIsAbsentFromJsonWhenNull() {
    ErrorResponse response = new ErrorResponse(400, ERROR_ATTRIBUTES, false);
    String json = JsonMapper.builder().build().writeValueAsString(response);

    assertThat(json).doesNotContain("\"message\"");
  }

  @Test
  void otherFieldsAreAlwaysPresent() {
    ErrorResponse response = new ErrorResponse(400, ERROR_ATTRIBUTES, false);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getError()).isEqualTo("Bad Request");
    assertThat(response.getPath()).isEqualTo("/test/path");
    assertThat(response.getTimestamp()).isNotNull();
  }
}
