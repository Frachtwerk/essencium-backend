package de.frachtwerk.essencium.backend.model.exception;

public class ResourceCannotCreateException extends ResourceException {

  private static final String DEFAULT_ACTION = "CREATE";

  public ResourceCannotCreateException(String resourceType, String identifier) {
    super(
        String.format("Cannot create resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier);
  }

  public ResourceCannotCreateException(String resourceType, String identifier, Throwable cause) {
    super(
        String.format("Cannot create resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier,
        cause);
  }

  public ResourceCannotCreateException(String message, String resourceType, String identifier) {
    super(message, resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotCreateException(
      String message, String resourceType, String identifier, Throwable cause) {
    super(message, resourceType, DEFAULT_ACTION, identifier, cause);
  }
}
