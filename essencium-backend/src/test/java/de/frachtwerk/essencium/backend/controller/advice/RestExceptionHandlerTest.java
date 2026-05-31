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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.dto.ErrorResponse;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class RestExceptionHandlerTest {

  @Test
  void formatValidationErrorUsesFieldArgumentWhenPresent() {
    var fieldArgument = new DefaultMessageSourceResolvable(new String[] {"name"}, "name");
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

  static Stream<Arguments> includeMessageCases() {
    return Stream.of(
        Arguments.of(ErrorProperties.IncludeAttribute.ALWAYS, null, true),
        Arguments.of(ErrorProperties.IncludeAttribute.NEVER, null, false),
        Arguments.of(ErrorProperties.IncludeAttribute.ON_PARAM, null, false),
        Arguments.of(ErrorProperties.IncludeAttribute.ON_PARAM, "true", true),
        Arguments.of(ErrorProperties.IncludeAttribute.ON_PARAM, "false", false));
  }

  @ParameterizedTest
  @MethodSource("includeMessageCases")
  void shouldIncludeMessage(
      ErrorProperties.IncludeAttribute mode, String messageParam, boolean expectMessagePresent) {
    ResponseEntity<Object> response = handleNotAllowedException(mode, messageParam);

    Object message = ((ErrorResponse) response.getBody()).getMessage();
    if (expectMessagePresent) {
      assertThat(message).isNotNull();
    } else {
      assertThat(message).isNull();
    }
  }

  private ResponseEntity<Object> handleNotAllowedException(
      ErrorProperties.IncludeAttribute mode, String messageParam) {
    ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("error", "Forbidden");
    attrs.put("message", "not allowed");
    attrs.put("path", "/test");
    attrs.put("timestamp", "2026-01-01T00:00");
    when(errorAttributes.getErrorAttributes(any(), any(ErrorAttributeOptions.class)))
        .thenReturn(attrs);

    ServerProperties serverProperties = new ServerProperties();
    serverProperties.getError().setIncludeMessage(mode);

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    httpRequest.setRequestURI("/test");
    if (messageParam != null) {
      httpRequest.setParameter("message", messageParam);
    }

    return new RestExceptionHandler(errorAttributes, serverProperties)
        .handleNotAllowedException(
            new NotAllowedException("not allowed"), new ServletWebRequest(httpRequest));
  }
}
