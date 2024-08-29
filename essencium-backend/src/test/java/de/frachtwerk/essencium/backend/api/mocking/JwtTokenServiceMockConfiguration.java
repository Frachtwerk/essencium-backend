package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import java.util.List;
import java.util.UUID;

public class JwtTokenServiceMockConfiguration implements MockConfiguration {

  private final JwtTokenService mockedObject;

  public JwtTokenServiceMockConfiguration(JwtTokenService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public JwtTokenServiceMockConfiguration returnDummyTokenOnGetTokenForUsername(String username) {
    doReturn(List.of(SessionToken.builder().type(SessionTokenType.REFRESH).build()))
        .when(mockedObject)
        .getTokens(username, SessionTokenType.REFRESH);
    return this;
  }

  public JwtTokenServiceMockConfiguration returnRandomTokenOnCreateToken() {
    doReturn(UUID.randomUUID().toString())
        .when(mockedObject)
        .createToken(any(), any(), any(), any(), any());
    return this;
  }
}
