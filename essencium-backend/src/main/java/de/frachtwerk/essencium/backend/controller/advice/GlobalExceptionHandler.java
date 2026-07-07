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

import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ProblemDetailFactory problemDetailFactory;

  public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
    this.problemDetailFactory = problemDetailFactory;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, exception.getMessage(), exception, request);
  }

  @ExceptionHandler(InvalidInputException.class)
  public ResponseEntity<ProblemDetail> handleInvalidInputException(
      InvalidInputException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.BAD_REQUEST,
        ErrorCode.INVALID_INPUT,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(ResourceUpdateException.class)
  public ResponseEntity<ProblemDetail> handleResourceUpdateException(
      ResourceUpdateException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.BAD_REQUEST,
        ErrorCode.INVALID_INPUT,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
      DuplicateResourceException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.CONFLICT,
        ErrorCode.DUPLICATE_RESOURCE,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(NotAllowedException.class)
  public ResponseEntity<ProblemDetail> handleNotAllowedException(
      NotAllowedException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, exception.getMessage(), exception, request);
  }

  @ExceptionHandler(TokenInvalidationException.class)
  public ResponseEntity<ProblemDetail> handleTokenInvalidationException(
      TokenInvalidationException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.TOKEN_INVALIDATION,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(TranslationFileException.class)
  public ResponseEntity<ProblemDetail> handleTranslationFileException(
      TranslationFileException exception, HttpServletRequest request) {
    log.error("Failed to process translation file", exception);
    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.TRANSLATION_FILE_ERROR,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
            .toList();

    ProblemDetail problemDetail =
        problemDetailFactory.create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_FAILED,
            "Validation failed",
            exception,
            request);

    problemDetailFactory.addFieldErrorsIfAllowed(problemDetail, fieldErrors, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception, HttpServletRequest request) {
    List<FieldErrorResponse> fieldErrors =
        exception.getParameterValidationResults().stream().flatMap(this::toFieldErrors).toList();

    ProblemDetail problemDetail =
        problemDetailFactory.create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_FAILED,
            "Validation failed",
            exception,
            request);

    problemDetailFactory.addFieldErrorsIfAllowed(problemDetail, fieldErrors, request);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_REQUEST,
        "Malformed request body",
        exception,
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    String sqlState = findSqlState(exception);

    if ("23505".equals(sqlState)) {
      return createResponse(
          HttpStatus.CONFLICT,
          ErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
          "Unique constraint violation",
          exception,
          request);
    }

    if ("23503".equals(sqlState)) {
      return createResponse(
          HttpStatus.CONFLICT,
          ErrorCode.FOREIGN_KEY_VIOLATION,
          "Foreign key violation",
          exception,
          request);
    }

    if ("23502".equals(sqlState)) {
      return createResponse(
          HttpStatus.BAD_REQUEST,
          ErrorCode.NOT_NULL_VIOLATION,
          "Not null violation",
          exception,
          request);
    }

    throw exception;
  }

  private ResponseEntity<ProblemDetail> createResponse(
      HttpStatus status,
      ErrorCode errorCode,
      String detail,
      Throwable throwable,
      HttpServletRequest request) {
    ProblemDetail problemDetail =
        problemDetailFactory.create(status, errorCode, detail, throwable, request);
    return ResponseEntity.status(status).body(problemDetail);
  }

  private Stream<FieldErrorResponse> toFieldErrors(ParameterValidationResult validationResult) {
    String field = validationResult.getMethodParameter().getParameterName();

    return validationResult.getResolvableErrors().stream()
        .map(MessageSourceResolvable::getDefaultMessage)
        .map(message -> new FieldErrorResponse(field, message));
  }

  private String findSqlState(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      if (current instanceof SQLException sqlException) {
        return sqlException.getSQLState();
      }

      current = current.getCause();
    }

    return null;
  }
}
