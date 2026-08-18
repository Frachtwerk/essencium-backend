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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private ProblemDetailFactory problemDetailFactory;

  @InjectMocks private GlobalExceptionHandler exceptionHandler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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
  void handleTokenInvalidationExceptionReturnsInternalServerErrorProblemDetail() {
    TokenInvalidationException exception =
        new TokenInvalidationException("Failed to invalidate tokens");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleTokenInvalidationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verify(problemDetailFactory)
        .create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.TOKEN_INVALIDATION,
            "Failed to invalidate tokens",
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
  void handleAuthenticationExceptionReturnsUnauthorizedProblemDetail() {
    AuthenticationException exception = new BadCredentialsException("Bad credentials");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleAuthenticationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(problemDetailFactory)
        .create(
            HttpStatus.UNAUTHORIZED,
            ErrorCode.AUTHENTICATION_FAILED,
            "Bad credentials",
            exception,
            request);
  }

  @Test
  void handleAccessDeniedExceptionReturnsForbiddenForAuthenticatedCaller() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "n/a", "READ"));
    AccessDeniedException exception = new AccessDeniedException("Access is denied");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleAccessDeniedException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(problemDetailFactory)
        .create(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access is denied", exception, request);
  }

  @Test
  void handleAccessDeniedExceptionReturnsUnauthorizedForAnonymousCaller() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ANONYMOUS")));
    AccessDeniedException exception = new AccessDeniedException("Access is denied");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleAccessDeniedException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void handleGenericExceptionReturnsInternalServerErrorProblemDetail() {
    Exception exception = new IllegalStateException("boom");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleGenericException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verify(problemDetailFactory)
        .create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR,
            "Internal server error",
            exception,
            request);
  }

  @Test
  void handleGenericExceptionUnwrapsAuthenticationExceptionFromCause() {
    AuthenticationException cause = new BadCredentialsException("Bad credentials");
    Exception exception = new IllegalStateException("wrapper", cause);

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleGenericException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void handleGenericExceptionUnwrapsAccessDeniedExceptionFromCause() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "n/a", "READ"));
    AccessDeniedException cause = new AccessDeniedException("Access is denied");
    Exception exception = new IllegalStateException("wrapper", cause);

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleGenericException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void resolveErrorCodeKeepsSpecificCodesForStatusesSharedBySeveralExceptions() {
    assertThat(
            exceptionHandler.resolveErrorCode(
                new HttpMessageNotReadableException("Malformed", null), HttpStatus.BAD_REQUEST))
        .isEqualTo(ErrorCode.MALFORMED_REQUEST);
    assertThat(
            exceptionHandler.resolveErrorCode(
                new BindException(this, "target"), HttpStatus.BAD_REQUEST))
        .isEqualTo(ErrorCode.VALIDATION_FAILED);
  }

  @Test
  void resolveErrorCodeFallsBackToTheStatusCode() {
    Exception exception = new IllegalStateException("boom");

    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.METHOD_NOT_ALLOWED))
        .isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.UNSUPPORTED_MEDIA_TYPE))
        .isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.NOT_ACCEPTABLE))
        .isEqualTo(ErrorCode.NOT_ACCEPTABLE);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.PAYLOAD_TOO_LARGE))
        .isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.NOT_FOUND))
        .isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.BAD_REQUEST))
        .isEqualTo(ErrorCode.INVALID_INPUT);
    assertThat(exceptionHandler.resolveErrorCode(exception, HttpStatus.SERVICE_UNAVAILABLE))
        .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
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
  void handleDataIntegrityViolationExceptionReturnsProblemDetailForUnknownSqlState() {
    DataIntegrityViolationException exception =
        dataIntegrityViolationExceptionWithSqlState("99999");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleDataIntegrityViolationException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verify(problemDetailFactory)
        .create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.DATA_INTEGRITY_VIOLATION,
            "Data integrity violation",
            exception,
            request);
  }

  private DataIntegrityViolationException dataIntegrityViolationExceptionWithSqlState(
      String sqlState) {
    return new DataIntegrityViolationException(
        "Data integrity violation", new SQLException("SQL", sqlState));
  }
}
