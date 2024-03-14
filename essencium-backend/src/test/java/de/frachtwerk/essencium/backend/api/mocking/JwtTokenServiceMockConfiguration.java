package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import java.util.List;

public class JwtTokenServiceMockConfiguration implements MockConfiguration {

  private final JwtTokenService mockedObject;

  public JwtTokenServiceMockConfiguration(JwtTokenService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public JwtTokenServiceMockConfiguration returnDummyTokenOnGetTokenForUsername(String username) {
    doReturn(List.of(SessionToken.builder().build())).when(mockedObject).getTokens(username);

    return this;
  }
}
