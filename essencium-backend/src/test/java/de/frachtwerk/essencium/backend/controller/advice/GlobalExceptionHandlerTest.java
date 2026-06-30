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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private ProblemDetailFactory problemDetailFactory;

  private GlobalExceptionHandler exceptionHandler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler(problemDetailFactory);
    request = new MockHttpServletRequest("GET", "/v1/test");

    lenient()
        .when(problemDetailFactory.create(any(), any(), anyString(), any(), any()))
        .thenAnswer(
            invocation -> {
              HttpStatus status = invocation.getArgument(0);
              String detail = invocation.getArgument(2);
              return ProblemDetail.forStatusAndDetail(status, detail);
            });
  }

  @Test
  void handleResourceNotFoundExceptionReturnsNotFoundProblemDetail() {
    ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleResourceNotFoundException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verify(problemDetailFactory)
        .create(
            HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Resource not found", exception, request);
  }

  @Test
  void handleInvalidInputExceptionReturnsBadRequestProblemDetail() {
    InvalidInputException exception = new InvalidInputException("Invalid input");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleInvalidInputException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(problemDetailFactory)
        .create(
            HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT, "Invalid input", exception, request);
  }

  @Test
  void handleResourceUpdateExceptionReturnsBadRequestProblemDetail() {
    ResourceUpdateException exception = new ResourceUpdateException("Update failed");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleResourceUpdateException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(problemDetailFactory)
        .create(
            HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT, "Update failed", exception, request);
  }

  @Test
  void handleDuplicateResourceExceptionReturnsConflictProblemDetail() {
    DuplicateResourceException exception = new DuplicateResourceException("Duplicate resource");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleDuplicateResourceException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(problemDetailFactory)
        .create(
            HttpStatus.CONFLICT,
            ErrorCode.DUPLICATE_RESOURCE,
            "Duplicate resource",
            exception,
            request);
  }

  @Test
  void handleNotAllowedExceptionReturnsForbiddenProblemDetail() {
    NotAllowedException exception = new NotAllowedException("Not allowed");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleNotAllowedException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(problemDetailFactory)
        .create(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Not allowed", exception, request);
  }

  @Test
  void handleTokenInvalidationExceptionReturnsUnauthorizedProblemDetail() {
    TokenInvalidationException exception = new TokenInvalidationException("Token invalid");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleTokenInvalidationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(problemDetailFactory)
        .create(
            HttpStatus.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALIDATION,
            "Token invalid",
            exception,
            request);
  }

  @Test
  void handleTranslationFileExceptionReturnsInternalServerErrorProblemDetail() {
    TranslationFileException exception = new TranslationFileException("Translation file error");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleTranslationFileException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verify(problemDetailFactory)
        .create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.TRANSLATION_FILE_ERROR,
            "Translation file error",
            exception,
            request);
  }

  @Test
  void handleHttpMessageNotReadableExceptionReturnsMalformedRequestProblemDetail() {
    HttpMessageNotReadableException exception =
        new HttpMessageNotReadableException("Malformed", null);

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleHttpMessageNotReadableException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(problemDetailFactory)
        .create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.MALFORMED_REQUEST,
            "Malformed request body",
            exception,
            request);
  }

  @Test
  void handleDataIntegrityViolationExceptionMapsUniqueConstraintViolation() {
    DataIntegrityViolationException exception =
        dataIntegrityViolationExceptionWithSqlState("23505");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleDataIntegrityViolationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(problemDetailFactory)
        .create(
            HttpStatus.CONFLICT,
            ErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
            "Unique constraint violation",
            exception,
            request);
  }

  @Test
  void handleDataIntegrityViolationExceptionMapsForeignKeyViolation() {
    DataIntegrityViolationException exception =
        dataIntegrityViolationExceptionWithSqlState("23503");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleDataIntegrityViolationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(problemDetailFactory)
        .create(
            HttpStatus.CONFLICT,
            ErrorCode.FOREIGN_KEY_VIOLATION,
            "Foreign key violation",
            exception,
            request);
  }

  @Test
  void handleDataIntegrityViolationExceptionMapsNotNullViolation() {
    DataIntegrityViolationException exception =
        dataIntegrityViolationExceptionWithSqlState("23502");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleDataIntegrityViolationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(problemDetailFactory)
        .create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.NOT_NULL_VIOLATION,
            "Not null violation",
            exception,
            request);
  }

  @Test
  void handleDataIntegrityViolationExceptionRethrowsUnknownSqlState() {
    DataIntegrityViolationException exception =
        dataIntegrityViolationExceptionWithSqlState("99999");

    assertThatThrownBy(
            () -> exceptionHandler.handleDataIntegrityViolationException(exception, request))
        .isSameAs(exception);
  }

  private DataIntegrityViolationException dataIntegrityViolationExceptionWithSqlState(
      String sqlState) {
    return new DataIntegrityViolationException(
        "Data integrity violation", new SQLException("SQL", sqlState));
  }
}
