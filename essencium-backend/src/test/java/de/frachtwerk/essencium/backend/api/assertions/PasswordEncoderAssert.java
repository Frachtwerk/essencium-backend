package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.AbstractAssert;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderAssert extends AbstractAssert<PasswordEncoderAssert, PasswordEncoder> {

  protected PasswordEncoderAssert(PasswordEncoder actual) {
    super(actual, PasswordEncoderAssert.class);
  }

  public void passwordUpdateMethodsAreTriggeredOnes() {
    verify(actual).matches(anyString(), anyString());
    verify(actual).encode(anyString());
  }
}
