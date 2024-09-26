package de.frachtwerk.essencium.backend.model.exception;

import java.util.Arrays;
import java.util.Map;

public class EssenciumException extends Exception implements ReportableException {
  public EssenciumException(String message) {
    super(message);
  }

  public EssenciumException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public Map<String, Object> reportInternals() {
    return Map.of(
        "internalErrorType", this.getClass().getSimpleName(), "internalErrorMessage", getMessage());
  }

  @Override
  public Map<String, Object> reportDebug() {
    return Map.of(
        "stackTrace",
        Arrays.stream(this.getStackTrace()).map(StackTraceElement::toString).toList());
  }
}
