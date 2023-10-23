package de.frachtwerk.essencium.backend.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.JwtConfigProperties;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.security.SessionTokenKeyLocator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ProtectedHeader;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

  @Mock SessionTokenRepository sessionTokenRepository;
  @Mock SessionTokenKeyLocator sessionTokenKeyLocator;
  JwtConfigProperties jwtConfigProperties;
  @Mock UserMailService userMailService;
  @Mock LongUserService userService;
  JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() {
    jwtConfigProperties = new JwtConfigProperties();
    jwtConfigProperties.setIssuer(RandomStringUtils.randomAlphanumeric(5, 10));
    jwtConfigProperties.setAccessTokenExpiration(86400);
    jwtConfigProperties.setRefreshTokenExpiration(2592000);
    jwtTokenService =
        new JwtTokenService(
            sessionTokenRepository, sessionTokenKeyLocator, jwtConfigProperties, userMailService);
    jwtTokenService.setUserService(userService);
  }

  @Test
  void createToken() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });

    String token = jwtTokenService.createToken(user, SessionTokenType.ACCESS, null, null);

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
  }

  @Test
  void verifyToken() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();

    final SessionToken[] sessionToken = {null};

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              sessionToken[0] = invocation.getArgument(0);
              sessionToken[0].setId(UUID.randomUUID());
              return sessionToken[0];
            });

    String token = jwtTokenService.createToken(user, SessionTokenType.REFRESH, null, null);

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(sessionToken[0].getKey());

    Claims claims = jwtTokenService.verifyToken(token);
    Date issuedAt = claims.getIssuedAt();
    Date expiresAt = claims.getExpiration();

    assertThat(claims.getIssuer(), Matchers.is(jwtConfigProperties.getIssuer()));
    assertThat(claims.getSubject(), Matchers.is(user.getUsername()));
    assertThat(claims.get("nonce", String.class), Matchers.is(user.getNonce()));
    assertThat(claims.get("given_name", String.class), Matchers.is(user.getFirstName()));
    assertThat(claims.get("family_name", String.class), Matchers.is(user.getLastName()));
    assertThat(claims.get("uid", Long.class), Matchers.is(user.getId()));
    assertThat(
        Duration.between(issuedAt.toInstant(), Instant.now()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.greaterThan(0), Matchers.lessThan(5 * 1000 * 1000) // no older than 5 seconds
            ));

    assertThat(
        Duration.between(Instant.now(), expiresAt.toInstant()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.lessThan(jwtConfigProperties.getAccessTokenExpiration() * 1000 * 1000),
            Matchers.greaterThan(jwtConfigProperties.getAccessTokenExpiration() - 5 * 1000 * 1000),
            Matchers.greaterThan(0)));
  }
}
