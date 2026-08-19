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

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class ProblemDetailWriterTest {

  private ProblemDetailWriter writer;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    WebProperties webProperties = new WebProperties();
    webProperties.getError().setIncludeMessage(ErrorProperties.IncludeAttribute.ALWAYS);

    writer =
        new ProblemDetailWriter(
            new ProblemDetailFactory(new EssenciumErrorProperties(), webProperties),
            new ObjectMapper());
    request = new MockHttpServletRequest("GET", "/v1/roles");
    response = new MockHttpServletResponse();
  }

  @Test
  void writesAProblemDetail() throws Exception {
    writer.write(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorCode.AUTHENTICATION_FAILED,
        "Token expired",
        null);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString())
        .contains("\"type\":\"urn:frachtwerk:error:AUTHENTICATION_FAILED\"")
        .contains("\"instance\":\"/v1/roles\"")
        .contains("\"detail\":\"Token expired\"")
        .contains("\"status\":401");
  }

  @Test
  void writesNothingOnACommittedResponse() throws Exception {
    response.setCommitted(true);

    writer.write(
        request, response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED, "x", null);

    assertThat(response.getContentAsString()).isEmpty();
  }
}
