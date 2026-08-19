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
import jakarta.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterErrors;
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
 * ResponseEntityExceptionHandler} so the Spring MVC exceptions keep their status code.
 *
 * <p>Deliberately has no {@code @ExceptionHandler(Exception.class)}: such a mapping would match
 * every exception, and {@code ExceptionHandlerMethodResolver} only walks {@code getCause()} when
 * the thrown type itself has no mapping. A wrapped exception would therefore never reach its
 * specific handler. The catch-all lives in {@link FallbackExceptionHandler}, which runs last.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 1)
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

  /**
   * Raised when the authentication backend itself fails (LDAP unreachable, database error), not
   * when a credential is rejected — the distinction is lost if this falls through to 401.
   */
  @ExceptionHandler(InternalAuthenticationServiceException.class)
  public ResponseEntity<ProblemDetail> handleInternalAuthenticationServiceException(
      InternalAuthenticationServiceException exception, HttpServletRequest request) {
    log.error("Authentication backend failed", exception);

    return createResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_SERVER_ERROR,
        exception.getMessage(),
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

  /**
   * Reproduces the status decision of {@code ExceptionTranslationFilter}, which no longer sees the
   * exception — not its delegation to {@code AccessDeniedHandler} / {@code
   * AuthenticationEntryPoint}, which stay unused for denials raised inside the dispatch.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      AccessDeniedException exception, HttpServletRequest request) {
    log.info("Access denied on {}: {}", request.getRequestURI(), exception.getMessage());

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
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpServletResponse response = ((ServletWebRequest) request).getResponse();

    if (response != null && response.isCommitted()) {
      log.warn("Response already committed, ignoring: {}", exception.toString());
      return null;
    }

    if (body == null && exception instanceof ErrorResponse errorResponse) {
      body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    }

    if (statusCode.is5xxServerError()) {
      log.error("Unhandled exception", exception);
      request.setAttribute(
          WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception, RequestAttributes.SCOPE_REQUEST);
    } else {
      log.debug("Request rejected with {}: {}", statusCode.value(), exception.toString());
    }

    ProblemDetail problemDetail =
        createProblemDetail(
            statusCode,
            resolveErrorCode(exception, statusCode),
            detailOf(body, exception),
            exception,
            servletRequest(request));

    return createResponseEntity(problemDetail, headers, statusCode, request);
  }

  protected ResponseEntity<ProblemDetail> createResponse(
      HttpStatusCode status,
      ProblemErrorCode errorCode,
      @Nullable String detail,
      Throwable throwable,
      HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(createProblemDetail(status, errorCode, detail, throwable, request));
  }

  /**
   * Every response of this advice passes through here, including the inherited Spring MVC types and
   * validation errors. Override to add or rewrite properties for all of them; for headers or status
   * override {@code createResponseEntity} instead.
   */
  protected ProblemDetail createProblemDetail(
      HttpStatusCode status,
      ProblemErrorCode errorCode,
      @Nullable String detail,
      Throwable throwable,
      HttpServletRequest request) {
    return problemDetailFactory.create(status, errorCode, detail, throwable, request);
  }

  /** Exception type wins over status code, so the 400s stay distinguishable. */
  protected ProblemErrorCode resolveErrorCode(Exception exception, HttpStatusCode statusCode) {
    if (exception instanceof HttpMessageNotReadableException) {
      return ErrorCode.MALFORMED_REQUEST;
    }

    if (exception instanceof BindException
        || exception instanceof HandlerMethodValidationException
        || exception instanceof MethodValidationException) {
      return ErrorCode.VALIDATION_FAILED;
    }

    return ErrorCode.forStatus(statusCode);
  }

  private ResponseEntity<Object> validationResponse(
      Exception exception,
      List<FieldErrorResponse> fieldErrors,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    HttpServletRequest servletRequest = servletRequest(request);

    ProblemDetail problemDetail =
        createProblemDetail(
            status, ErrorCode.VALIDATION_FAILED, "Validation failed", exception, servletRequest);

    problemDetailFactory.addFieldErrorsIfAllowed(problemDetail, fieldErrors, servletRequest);

    return createResponseEntity(problemDetail, headers, status, request);
  }

  /**
   * A {@code null} detail is intentional: {@code ProblemDetailFactory} replaces it with the generic
   * text. Falling back to {@code getMessage()} would surface the status prefix a {@code
   * ResponseStatusException} without a reason carries ("404 NOT_FOUND").
   */
  private static @Nullable String detailOf(@Nullable Object body, Exception exception) {
    if (body instanceof ProblemDetail problemDetail) {
      return problemDetail.getDetail();
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
    return ((ParameterErrors) validationResult)
        .getFieldErrors().stream()
            .map(
                messageSourceResolvable ->
                    new FieldErrorResponse(
                        messageSourceResolvable.getField(),
                        messageSourceResolvable.getDefaultMessage()));
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
