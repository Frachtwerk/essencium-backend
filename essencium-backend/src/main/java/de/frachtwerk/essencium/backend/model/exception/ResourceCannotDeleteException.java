package de.frachtwerk.essencium.backend.model.exception;

public class ResourceCannotDeleteException extends ResourceException {

  private static final String DEFAULT_ACTION = "DELETE";

  public ResourceCannotDeleteException(String resourceType, String identifier) {
    super(
        String.format("Cannot delete resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier);
  }

  public ResourceCannotDeleteException(String resourceType, String identifier, Throwable cause) {
    super(
        String.format("Cannot delete resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier,
        cause);
  }

  public ResourceCannotDeleteException(String message, String resourceType, String identifier) {
    super(message, resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotDeleteException(
      String message, String resourceType, String identifier, Throwable cause) {
    super(message, resourceType, DEFAULT_ACTION, identifier, cause);
  }
}
