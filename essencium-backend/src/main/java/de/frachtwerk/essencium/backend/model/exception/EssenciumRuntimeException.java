package de.frachtwerk.essencium.backend.model.exception;

import java.util.Arrays;
import java.util.Map;

public class EssenciumRuntimeException extends RuntimeException implements ReportableException {
  public EssenciumRuntimeException(String message) {
    super(message);
  }

  public EssenciumRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public Map<String, Object> reportInternals() {
    return Map.of(
        "internalErrorType", this.getClass().getName(), "internalErrorMessage", getMessage());
  }

  @Override
  public Map<String, Object> reportDebug() {
    return Map.of(
        "stackTrace",
        Arrays.stream(this.getStackTrace()).map(StackTraceElement::toString).toList());
  }
}
