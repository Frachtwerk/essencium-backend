/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  private final ErrorAttributes errorAttributes;

  @Autowired
  public RestExceptionHandler(ErrorAttributes errorAttributes) {
    this.errorAttributes = errorAttributes;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    final var error = status.value();
    final var path = ((ServletWebRequest) request).getRequest().getRequestURI();
    final var message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> String.format("%s %s", e.getField(), e.getDefaultMessage()))
            .findFirst()
            .orElse(null);

    final var attrs = errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attrs.put("error", error);
    attrs.put("message", message);
    attrs.put("path", path);

    return new ResponseEntity<>(new ErrorResponse(status.value(), attrs), status);
  }
}
