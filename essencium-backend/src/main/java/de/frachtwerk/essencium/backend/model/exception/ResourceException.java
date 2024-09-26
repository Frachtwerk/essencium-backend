package de.frachtwerk.essencium.backend.model.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ResourceException extends EssenciumRuntimeException implements ReportableException {

  private final String resourceType; // Keep resourceType as EntityTypeEnum
  private final String action;
  private final String identifier;

  public static final String UNKNOWN_TYPE = "[unknown]";

  public ResourceException(String message, String resourceType, String action, String identifier) {
    super(message);
    this.resourceType = resourceType != null ? resourceType : UNKNOWN_TYPE;
    this.action = action;
    this.identifier = identifier;
  }

  public ResourceException(
      String message, String resourceType, String action, String identifier, Throwable cause) {
    super(message, cause);
    this.resourceType = resourceType != null ? resourceType : UNKNOWN_TYPE;
    this.action = action;
    this.identifier = identifier;
  }

  @Override
  public String getMessage() {
    String baseMessage =
        String.format(
            "Error during '%s' action on resource with identifier '%s': %s",
            action, identifier, super.getMessage());

    if (!Objects.equals(resourceType, UNKNOWN_TYPE)) {
      return String.format(
          "Error during '%s' action on resource type '%s' with identifier '%s': %s",
          action, resourceType, identifier, super.getMessage());
    }

    return baseMessage;
  }

  @Override
  public Map<String, Object> reportInternals() {
    HashMap<String, Object> result = new HashMap<>(super.reportInternals());
    result.put("resourceType", resourceType);
    result.put("action", action);
    result.put("identifier", identifier);
    return result;
  }
}
