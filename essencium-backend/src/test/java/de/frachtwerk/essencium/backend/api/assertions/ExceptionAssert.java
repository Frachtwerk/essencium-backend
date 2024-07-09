package de.frachtwerk.essencium.backend.api.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class ExceptionAssert extends AbstractAssert<ExceptionAssert, Exception> {
  protected ExceptionAssert(Exception actual) {
    super(actual, ExceptionAssert.class);
  }

  public void hasMessageContaining(String expectedPart) {
    Assertions.assertThat(actual).hasMessageContaining(expectedPart);
  }
}
