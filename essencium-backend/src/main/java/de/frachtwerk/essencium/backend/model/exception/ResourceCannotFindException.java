package de.frachtwerk.essencium.backend.model.exception;

public class ResourceCannotFindException extends ResourceException {

  private static final String DEFAULT_ACTION = "FIND";

  public ResourceCannotFindException(String resourceType, String identifier) {
    super(
        String.format("Cannot find resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier);
  }

  public ResourceCannotFindException(String identifier) {
    super(
        String.format("Cannot find resource with ID '%s'", identifier),
        "Unknown",
        DEFAULT_ACTION,
        identifier);
  }

  public ResourceCannotFindException(String resourceType, String identifier, Throwable cause) {
    super(
        String.format("Cannot find resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier,
        cause);
  }

  public ResourceCannotFindException(String message, String resourceType, String identifier) {
    super(message, resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotFindException(
      String message, String resourceType, String identifier, Throwable cause) {
    super(message, resourceType, DEFAULT_ACTION, identifier, cause);
  }
}
