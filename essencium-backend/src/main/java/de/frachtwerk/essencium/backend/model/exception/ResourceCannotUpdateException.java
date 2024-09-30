package de.frachtwerk.essencium.backend.model.exception;

public class ResourceCannotUpdateException extends ResourceException {

  private static final String DEFAULT_ACTION = "UPDATE";

  public ResourceCannotUpdateException(String resourceType, String identifier) {
    super(
        String.format("Cannot update resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier);
  }

  public ResourceCannotUpdateException(String resourceType, String identifier, Throwable cause) {
    super(
        String.format("Cannot update resource with ID '%s'", identifier),
        resourceType,
        DEFAULT_ACTION,
        identifier,
        cause);
  }

  public ResourceCannotUpdateException(String message, String resourceType, String identifier) {
    super(message, resourceType, DEFAULT_ACTION, identifier);
  }

  public ResourceCannotUpdateException(
      String message, String resourceType, String identifier, Throwable cause) {
    super(message, resourceType, DEFAULT_ACTION, identifier, cause);
  }
}
