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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FieldErrorResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldNotSerializeNullFieldAndMessage() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new FieldErrorResponse(null, null));

    assertFalse(json.contains("field"));
    assertFalse(json.contains("message"));
  }

  @Test
  void shouldSerializeNonNullFieldAndMessage() throws JsonProcessingException {
    String json =
        objectMapper.writeValueAsString(new FieldErrorResponse("email", "must not be blank"));

    assertTrue(json.contains("\"field\":\"email\""));
    assertTrue(json.contains("\"message\":\"must not be blank\""));
  }
}
