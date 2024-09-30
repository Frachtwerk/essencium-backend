package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.*;
import de.frachtwerk.essencium.backend.model.exception.response.EssenciumExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

public class GlobalExceptionHandlerImp extends GlobalExceptionHandler {

  private final ExceptionToResponseConverter<ReportableException, EssenciumExceptionResponse>
      exceptionConverter;
  private final ExceptionToStatusMapper exceptionToStatusMapper;

  public GlobalExceptionHandlerImp(
      ExceptionToResponseConverter<ReportableException, EssenciumExceptionResponse>
          exceptionConverter,
      ExceptionToStatusMapper exceptionToStatusMapper) {
    this.exceptionConverter = exceptionConverter;
    this.exceptionToStatusMapper = exceptionToStatusMapper;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    final var message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> String.format("%s %s", e.getField(), e.getDefaultMessage()))
            .findFirst()
            .orElse(null);

    return new ResponseEntity<>(
        exceptionConverter.convert(
            new EssenciumException(message),
            HttpStatus.resolve(status.value()),
            (HttpServletRequest) request),
        status);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    HandlerMethodValidationExceptionWrapper wrappedException =
        new HandlerMethodValidationExceptionWrapper(exception, exception.getMessage());

    return new ResponseEntity<>(
        exceptionConverter.convert(
            wrappedException, HttpStatus.resolve(status.value()), (HttpServletRequest) request),
        status);
  }

  @ExceptionHandler(EssenciumException.class)
  public ResponseEntity<Object> handleEssenciumException(
      EssenciumException e, HttpServletRequest request) {
    HttpStatus status = exceptionToStatusMapper.map(e);
    return new ResponseEntity<>(exceptionConverter.convert(e, status, request), status);
  }

  @ExceptionHandler(EssenciumRuntimeException.class)
  public ResponseEntity<Object> handleEssenciumException(
      EssenciumRuntimeException e, HttpServletRequest request) {
    HttpStatus status = exceptionToStatusMapper.map(e);
    return new ResponseEntity<>(exceptionConverter.convert(e, status, request), status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleException(Exception e, HttpServletRequest request) {
    JavaExceptionWrapper wrappedException = new JavaExceptionWrapper(e);
    HttpStatus status = exceptionToStatusMapper.map(e);
    return new ResponseEntity<>(
        exceptionConverter.convert(wrappedException, status, request), status);
  }

  private record JavaExceptionWrapper(Exception exception) implements ReportableException {

    @Override
    public Map<String, Object> reportInternals() {
      return Map.of(
          "internalErrorType", exception.getClass().getSimpleName(),
          "internalErrorMessage", exception.getMessage());
    }

    @Override
    public Map<String, Object> reportDebug() {
      return Map.of(
          "stackTrace",
          Arrays.stream(this.exception.getStackTrace()).map(StackTraceElement::toString).toList());
    }
  }

  private static class HandlerMethodValidationExceptionWrapper extends EssenciumException {
    private final HandlerMethodValidationException exception;

    public HandlerMethodValidationExceptionWrapper(
        HandlerMethodValidationException exception, String message) {
      super(message);
      this.exception = exception;
    }

    @Override
    public Map<String, Object> reportInternals() {
      HashMap<String, Object> result = new HashMap<>(super.reportInternals());

      List<String> errors =
          this.exception.getAllValidationResults().stream()
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

      result.put("errors", errors);

      return result;
    }
  }
}
