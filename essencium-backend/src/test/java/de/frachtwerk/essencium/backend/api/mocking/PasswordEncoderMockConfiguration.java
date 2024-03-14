package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderMockConfiguration implements MockConfiguration {

  private final PasswordEncoder mockedObject;

  public PasswordEncoderMockConfiguration(PasswordEncoder mockedObject) {
    this.mockedObject = mockedObject;
  }

  public PasswordEncoderMockConfiguration writePassedPasswordInAndReturn(
      AtomicReference<String> capturedPassword, String encodedPassword) {
    doAnswer(
            invocation -> {
              capturedPassword.set(invocation.getArgument(0));
              return encodedPassword;
            })
        .when(mockedObject)
        .encode(anyString());
    return this;
  }

  public PasswordEncoderMockConfiguration returnEncodedPasswordWhenPasswordGiven(
      String encodedPassword, String password) {
    doReturn(encodedPassword).when(mockedObject).encode(password);

    return this;
  }

  public PasswordEncoderMockConfiguration passGivenPassword(
      CharSequence password, String passwordHash) {
    doReturn(true).when(mockedObject).matches(password, passwordHash);

    return this;
  }
}
