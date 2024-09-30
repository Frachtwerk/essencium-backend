package de.frachtwerk.essencium.backend.model.exception;

import org.springframework.data.jpa.domain.Specification;

public class ResourceCannotFindSpecificationException extends ResourceCannotFindException {

  private final Specification<?> specification;

  public ResourceCannotFindSpecificationException(Specification<?> spec) {
    super(spec.toString());
    this.specification = spec;
  }

  @Override
  public String getMessage() {
    if (specification == null) {
      return super.getMessage();
    }
    return String.format("Cannot find according to specification: %s", specification);
  }
}
