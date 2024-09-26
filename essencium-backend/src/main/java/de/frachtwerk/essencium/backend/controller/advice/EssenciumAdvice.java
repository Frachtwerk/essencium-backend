package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(annotations = EnableEssenciumAdvice.class)
public class EssenciumAdvice {

  private final ExceptionToResponseConverterImp exceptionConverter;
  private final ExceptionToStatusMapper exceptionToStatusMapper;

  public EssenciumAdvice(
      ExceptionToResponseConverterImp exceptionConverter,
      ExceptionToStatusMapper exceptionToStatusMapper) {
    this.exceptionConverter = exceptionConverter;
    this.exceptionToStatusMapper = exceptionToStatusMapper;
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
}
