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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

/**
 * Maps exceptions to RFC 9457 {@link ProblemDetail} responses. Extends {@link
 * ResponseEntityExceptionHandler} so the Spring MVC exceptions keep their status code instead of
 * being swallowed by the {@code Exception} handler below.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";
  private static final String SQLSTATE_FOREIGN_KEY_VIOLATION = "23503";
  private static final String SQLSTATE_NOT_NULL_VIOLATION = "23502";

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
    log.error("Failed to invalidate tokens", exception);

    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    String sqlState = findSqlState(exception);

    if (SQLSTATE_UNIQUE_VIOLATION.equals(sqlState)) {
      return createResponse(
          HttpStatus.CONFLICT,
          ErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
          "Unique constraint violation",
          exception,
          request);
    }

    if (SQLSTATE_FOREIGN_KEY_VIOLATION.equals(sqlState)) {
      return createResponse(
          HttpStatus.CONFLICT,
          ErrorCode.FOREIGN_KEY_VIOLATION,
          "Foreign key violation",
          exception,
          request);
    }

    if (SQLSTATE_NOT_NULL_VIOLATION.equals(sqlState)) {
      return createResponse(
          HttpStatus.BAD_REQUEST,
          ErrorCode.NOT_NULL_VIOLATION,
          "Not null violation",
          exception,
          request);
    }

    log.error("Unhandled data integrity violation", exception);

    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.DATA_INTEGRITY_VIOLATION,
        "Data integrity violation",
        exception,
        request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException exception, HttpServletRequest request) {
    return createResponse(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.AUTHENTICATION_FAILED,
        exception.getMessage(),
        exception,
        request);
  }

  /** Mirrors {@code ExceptionTranslationFilter}, which no longer sees the exception. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      AccessDeniedException exception, HttpServletRequest request) {
    if (isAuthenticated(SecurityContextHolder.getContext().getAuthentication())) {
      return createResponse(
          HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, exception.getMessage(), exception, request);
    }

    return createResponse(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.AUTHENTICATION_FAILED,
        exception.getMessage(),
        exception,
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception exception, HttpServletRequest request) {
    AuthenticationException authenticationException =
        findCause(exception, AuthenticationException.class);

    if (authenticationException != null) {
      return handleAuthenticationException(authenticationException, request);
    }

    AccessDeniedException accessDeniedException = findCause(exception, AccessDeniedException.class);

    if (accessDeniedException != null) {
      return handleAccessDeniedException(accessDeniedException, request);
    }

    log.error("Unhandled exception", exception);

    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_SERVER_ERROR,
        "Internal server error",
        exception,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
            .toList();

    return validationResponse(exception, fieldErrors, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldErrorResponse> fieldErrors =
        exception.getParameterValidationResults().stream().flatMap(this::toFieldErrors).toList();

    return validationResponse(exception, fieldErrors, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpServletRequest servletRequest = servletRequest(request);

    if (statusCode.is5xxServerError()) {
      log.error("Unhandled exception", exception);
      request.setAttribute(
          WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception, RequestAttributes.SCOPE_REQUEST);
    }

    ProblemDetail problemDetail =
        problemDetailFactory.create(
            statusCode,
            resolveErrorCode(exception, statusCode),
            detailOf(body, exception),
            exception,
            servletRequest);

    return ResponseEntity.status(statusCode).headers(headers).body(problemDetail);
  }

  protected ResponseEntity<ProblemDetail> createResponse(
      HttpStatusCode status,
      ProblemErrorCode errorCode,
      @Nullable String detail,
      Throwable throwable,
      HttpServletRequest request) {
    ProblemDetail problemDetail =
        problemDetailFactory.create(status, errorCode, detail, throwable, request);
    return ResponseEntity.status(status).body(problemDetail);
  }

  /** Exception type wins over status code, so the 400s stay distinguishable. */
  protected ProblemErrorCode resolveErrorCode(Exception exception, HttpStatusCode statusCode) {
    if (exception instanceof HttpMessageNotReadableException) {
      return ErrorCode.MALFORMED_REQUEST;
    }

    if (exception instanceof MethodArgumentNotValidException
        || exception instanceof HandlerMethodValidationException
        || exception instanceof MethodValidationException
        || exception instanceof BindException) {
      return ErrorCode.VALIDATION_FAILED;
    }

    if (statusCode.is5xxServerError()) {
      return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    return switch (statusCode.value()) {
      case 404 -> ErrorCode.NOT_FOUND;
      case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
      case 406 -> ErrorCode.NOT_ACCEPTABLE;
      case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
      case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
      default -> ErrorCode.INVALID_INPUT;
    };
  }

  private ResponseEntity<Object> validationResponse(
      Exception exception,
      List<FieldErrorResponse> fieldErrors,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    HttpServletRequest servletRequest = servletRequest(request);

    ProblemDetail problemDetail =
        problemDetailFactory.create(
            status, ErrorCode.VALIDATION_FAILED, "Validation failed", exception, servletRequest);

    problemDetailFactory.addFieldErrorsIfAllowed(problemDetail, fieldErrors, servletRequest);

    return ResponseEntity.status(status).headers(headers).body(problemDetail);
  }

  /** {@code getMessage()} carries the status code as a prefix, the problem detail does not. */
  private static @Nullable String detailOf(@Nullable Object body, Exception exception) {
    if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
      return problemDetail.getDetail();
    }

    if (exception instanceof ErrorResponse errorResponse
        && errorResponse.getBody().getDetail() != null) {
      return errorResponse.getBody().getDetail();
    }

    return exception.getMessage();
  }

  private static HttpServletRequest servletRequest(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest();
    }

    throw new IllegalStateException(
        "GlobalExceptionHandler requires a ServletWebRequest but got " + request.getClass());
  }

  private static boolean isAuthenticated(@Nullable Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private Stream<FieldErrorResponse> toFieldErrors(ParameterValidationResult validationResult) {
    String field = validationResult.getMethodParameter().getParameterName();

    return validationResult.getResolvableErrors().stream()
        .map(MessageSourceResolvable::getDefaultMessage)
        .map(message -> new FieldErrorResponse(field, message));
  }

  private <T extends Throwable> @Nullable T findCause(Throwable throwable, Class<T> causeType) {
    Throwable current = throwable;

    while (current != null) {
      if (causeType.isInstance(current)) {
        return causeType.cast(current);
      }

      current = current.getCause();
    }

    return null;
  }

  private @Nullable String findSqlState(Throwable throwable) {
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
