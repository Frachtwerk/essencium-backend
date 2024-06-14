package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import java.time.LocalDate;
import java.util.List;

public class JwtTokenServiceMockConfiguration implements MockConfiguration {

  private final JwtTokenService mockedObject;

  public JwtTokenServiceMockConfiguration(JwtTokenService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public JwtTokenServiceMockConfiguration returnDummyTokenOnGetTokenForUsername(
      String username, SessionTokenType type) {
    doReturn(List.of(SessionToken.builder().build())).when(mockedObject).getTokens(username, type);

    return this;
  }

  public JwtTokenServiceMockConfiguration returnDummyTokenOnCreateTokenForApiUser() {
    doReturn("token")
        .when(mockedObject)
        .createToken(
            any(ApiTokenUser.class),
            any(SessionTokenType.class),
            any(),
            any(),
            any(LocalDate.class));

    return this;
  }

  public JwtTokenServiceMockConfiguration doNothingOnDeleteByUsername(String apiTokenUsername) {
    doNothing().when(mockedObject).deleteAllByUsername(apiTokenUsername);

    return this;
  }
}
