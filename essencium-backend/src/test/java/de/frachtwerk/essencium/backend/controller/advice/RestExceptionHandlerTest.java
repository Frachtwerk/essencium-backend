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
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class RestExceptionHandlerTest {

  private RestExceptionHandler newHandler() {
    ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
    when(errorAttributes.getErrorAttributes(any(), any(ErrorAttributeOptions.class)))
        .thenAnswer(invocation -> new java.util.HashMap<String, Object>());
    return new RestExceptionHandler(errorAttributes);
  }

  private ServletWebRequest newRequest() {
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getRequestURI()).thenReturn("/api/v1/test");
    return new ServletWebRequest(httpRequest);
  }

  private HandlerMethodValidationException buildException(MessageSourceResolvable resolvable) {
    ParameterValidationResult result = mock(ParameterValidationResult.class);
    when(result.getResolvableErrors()).thenReturn(List.of(resolvable));
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    when(ex.getParameterValidationResults()).thenReturn(List.of(result));
    when(ex.getMessage()).thenReturn("Validation failed");
    return ex;
  }

  @SuppressWarnings("unchecked")
  @Test
  void handleHandlerMethodValidationException_withFieldArgument_formatsFieldAndMessage() {
    DefaultMessageSourceResolvable fieldArg =
        new DefaultMessageSourceResolvable(new String[] {"field"}, "username");
    DefaultMessageSourceResolvable resolvable =
        new DefaultMessageSourceResolvable(
            new String[] {"NotBlank"}, new Object[] {fieldArg}, "must not be blank");

    ResponseEntity<Object> response =
        newHandler()
            .handleHandlerMethodValidationException(
                buildException(resolvable),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                newRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = (ErrorResponse) response.getBody();
    assertThat(body).isNotNull();
    List<String> messages = (List<String>) body.getMessage();
    assertThat(messages).containsExactly("username must not be blank");
  }

  @SuppressWarnings("unchecked")
  @Test
  void handleHandlerMethodValidationException_withNullArguments_doesNotCrash() {
    DefaultMessageSourceResolvable resolvable =
        new DefaultMessageSourceResolvable(new String[] {"Custom"}, null, "must match pattern");

    ResponseEntity<Object> response =
        newHandler()
            .handleHandlerMethodValidationException(
                buildException(resolvable),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                newRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = (ErrorResponse) response.getBody();
    assertThat(body).isNotNull();
    List<String> messages = (List<String>) body.getMessage();
    assertThat(messages).containsExactly("must match pattern");
  }

  @SuppressWarnings("unchecked")
  @Test
  void handleHandlerMethodValidationException_withEmptyArguments_doesNotCrash() {
    DefaultMessageSourceResolvable resolvable =
        new DefaultMessageSourceResolvable(
            new String[] {"Custom"}, new Object[0], "must match pattern");

    ResponseEntity<Object> response =
        newHandler()
            .handleHandlerMethodValidationException(
                buildException(resolvable),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                newRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = (ErrorResponse) response.getBody();
    assertThat(body).isNotNull();
    List<String> messages = (List<String>) body.getMessage();
    assertThat(messages).containsExactly("must match pattern");
  }
}
