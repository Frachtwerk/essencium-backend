package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.*;

import org.springframework.core.env.Environment;

public class EnvironmentMockConfiguration implements MockConfiguration {

  private final Environment mockedObject;

  public EnvironmentMockConfiguration(Environment mockedObject) {
    this.mockedObject = mockedObject;
  }

  public EnvironmentMockConfiguration returnPropertyForGiveKey(String key, String value) {
    doReturn(value).when(mockedObject).getProperty(key);

    return this;
  }

  public EnvironmentMockConfiguration enableEmailValidation() {
    return returnPropertyForGiveKey("app.security.e-mail-validation-enabled", "true");
  }

  public EnvironmentMockConfiguration disableEmailValidation() {
    return returnPropertyForGiveKey("app.security.e-mail-validation-enabled", "false");
  }
}
