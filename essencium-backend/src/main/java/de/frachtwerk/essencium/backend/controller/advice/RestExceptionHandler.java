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

import de.frachtwerk.essencium.backend.model.dto.ErrorResponse;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

    Map<String, Object> attributes =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attributes.put("timestamp", LocalDateTime.now());
    attributes.put("error", error);
    attributes.put("message", message);
    attributes.put("path", path);

    return new ResponseEntity<>(new ErrorResponse(status.value(), attributes), status);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String path = ((ServletWebRequest) request).getRequest().getRequestURI();
    List<String> errors =
        ex.getParameterValidationResults().stream()
            .flatMap(e -> e.getResolvableErrors().stream())
            .map(
                messageSourceResolvable -> {
                  String message = messageSourceResolvable.getDefaultMessage();
                  DefaultMessageSourceResolvable messageSourceResolvableArgument =
                      (DefaultMessageSourceResolvable) messageSourceResolvable.getArguments()[0];
                  String field = messageSourceResolvableArgument.getDefaultMessage();
                  return String.format("%s %s", field, message);
                })
            .toList();

    Map<String, Object> attributes =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attributes.put("timestamp", LocalDateTime.now());
    attributes.put("error", ex.getMessage());
    attributes.put("message", errors);
    attributes.put("path", path);

    return new ResponseEntity<>(new ErrorResponse(status.value(), attributes), headers, status);
  }

  @ExceptionHandler(NotAllowedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<Object> handleNotAllowedException(
      NotAllowedException ex, WebRequest request) {
    String path = ((ServletWebRequest) request).getRequest().getRequestURI();

    Map<String, Object> attributes =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attributes.put("timestamp", LocalDateTime.now());
    attributes.put("error", HttpStatus.FORBIDDEN.value());
    attributes.put("message", ex.getMessage());
    attributes.put("path", path);

    return new ResponseEntity<>(
        new ErrorResponse(HttpStatus.FORBIDDEN.value(), attributes), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<Object> handleDuplicateResourceException(
      DuplicateResourceException ex, WebRequest request) {
    String path = ((ServletWebRequest) request).getRequest().getRequestURI();

    Map<String, Object> attributes =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attributes.put("timestamp", LocalDateTime.now());
    attributes.put("error", HttpStatus.CONFLICT.value());
    attributes.put("message", ex.getMessage());
    attributes.put("path", path);

    return new ResponseEntity<>(
        new ErrorResponse(HttpStatus.CONFLICT.value(), attributes), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InvalidInputException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> handleInvalidInputException(
      InvalidInputException ex, WebRequest request) {
    String path = ((ServletWebRequest) request).getRequest().getRequestURI();

    Map<String, Object> attributes =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    attributes.put("timestamp", LocalDateTime.now());
    attributes.put("error", HttpStatus.BAD_REQUEST.value());
    attributes.put("message", ex.getMessage());
    attributes.put("path", path);

    return new ResponseEntity<>(
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), attributes), HttpStatus.BAD_REQUEST);
  }
}
