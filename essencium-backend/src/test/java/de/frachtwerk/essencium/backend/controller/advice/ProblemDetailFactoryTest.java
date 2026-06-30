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
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class ProblemDetailFactoryTest {

  private final EssenciumErrorProperties errorProperties = new EssenciumErrorProperties();
  private final WebProperties webProperties = new WebProperties();

  @BeforeEach
  void setUp() {
    webProperties.getError().setIncludeMessage(ErrorProperties.IncludeAttribute.ALWAYS);
    webProperties.getError().setIncludeStacktrace(ErrorProperties.IncludeAttribute.NEVER);
  }

  @Test
  void createUsesConfiguredUrnPrefixAndRequestPath() {
    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INPUT,
            "Invalid input",
            new IllegalArgumentException("Invalid input"),
            request("/v1/test"));

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemDetail.getTitle()).isEqualTo("Bad Request");
    assertThat(problemDetail.getDetail()).isEqualTo("Invalid input");
    assertThat(problemDetail.getType().toString()).isEqualTo("urn:frachtwerk:error:INVALID_INPUT");
    assertThat(problemDetail.getInstance().toString()).isEqualTo("/v1/test");
    assertThat(problemDetail.getProperties()).containsKey("timestamp");
  }

  @Test
  void createUsesGenericDetailWhenIncludeMessageIsNever() {
    webProperties.getError().setIncludeMessage(ErrorProperties.IncludeAttribute.NEVER);

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.FORBIDDEN,
            ErrorCode.FORBIDDEN,
            "Forbidden detail",
            new RuntimeException("Forbidden detail"),
            request("/v1/users/1"));

    assertThat(problemDetail.getDetail()).isEqualTo("An error occurred");
  }

  @Test
  void createIncludesDetailOnParamWhenMessageParameterIsEnabled() {
    webProperties.getError().setIncludeMessage(ErrorProperties.IncludeAttribute.ON_PARAM);

    MockHttpServletRequest request = request("/v1/users/1");
    request.addParameter("message", "true");

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.FORBIDDEN,
            ErrorCode.FORBIDDEN,
            "Forbidden detail",
            new RuntimeException("Forbidden detail"),
            request);

    assertThat(problemDetail.getDetail()).isEqualTo("Forbidden detail");
  }

  @Test
  void createUsesGenericDetailOnParamWhenMessageParameterIsMissing() {
    webProperties.getError().setIncludeMessage(ErrorProperties.IncludeAttribute.ON_PARAM);

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.FORBIDDEN,
            ErrorCode.FORBIDDEN,
            "Forbidden detail",
            new RuntimeException("Forbidden detail"),
            request("/v1/users/1"));

    assertThat(problemDetail.getDetail()).isEqualTo("An error occurred");
  }

  @Test
  void createDoesNotIncludeStackTraceWhenTraceParameterIsMissing() {
    webProperties.getError().setIncludeStacktrace(ErrorProperties.IncludeAttribute.ON_PARAM);

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INPUT,
            "Invalid input",
            new IllegalArgumentException("Invalid input"),
            request("/v1/test"));

    assertThat(problemDetail.getProperties()).doesNotContainKey("stackTrace");
  }

  @Test
  void createIncludesStackTraceWhenTraceParameterIsEnabled() {
    webProperties.getError().setIncludeStacktrace(ErrorProperties.IncludeAttribute.ON_PARAM);

    MockHttpServletRequest request = request("/v1/test");
    request.addParameter("trace", "true");

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INPUT,
            "Invalid input",
            new IllegalArgumentException("Invalid input"),
            request);

    assertThat(problemDetail.getProperties()).containsKey("stackTrace");
    assertThat(problemDetail.getProperties().get("stackTrace").toString())
        .contains("IllegalArgumentException");
  }

  @Test
  void createDoesNotIncludeStackTraceWhenTraceParameterIsFalse() {
    webProperties.getError().setIncludeStacktrace(ErrorProperties.IncludeAttribute.ON_PARAM);

    MockHttpServletRequest request = request("/v1/test");
    request.addParameter("trace", "false");

    ProblemDetail problemDetail =
        createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INPUT,
            "Invalid input",
            new IllegalArgumentException("Invalid input"),
            request);

    assertThat(problemDetail.getProperties()).doesNotContainKey("stackTrace");
  }

  private ProblemDetail createProblemDetail(
      HttpStatus status,
      ErrorCode errorCode,
      String detail,
      Throwable throwable,
      MockHttpServletRequest request) {
    return new ProblemDetailFactory(errorProperties, webProperties)
        .create(status, errorCode, detail, throwable, request);
  }

  private MockHttpServletRequest request(String requestUri) {
    return new MockHttpServletRequest("GET", requestUri);
  }
}
