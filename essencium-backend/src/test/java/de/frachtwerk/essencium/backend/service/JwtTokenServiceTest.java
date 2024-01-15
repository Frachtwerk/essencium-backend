/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.service.JwtTokenService.*;
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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
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
  void loginTest() {
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

    String token = jwtTokenService.login(user, "test");

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
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

  @Test
  void renewTest() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();
    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    String token =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionToken.getType().name())
            .and()
            .subject(sessionToken.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(jwtConfigProperties.getIssuer())
            .claim(CLAIM_NONCE, user.getNonce())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .signWith(sessionToken.getKey())
            .compact();

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken1 = invocation.getArgument(0);
              sessionToken1.setId(UUID.randomUUID());
              return sessionToken1;
            });
    when(userService.loadUserByUsername(user.getUsername())).thenReturn(user);
    when(sessionTokenRepository.findAllByParentToken(sessionToken))
        .thenReturn(
            List.of(
                SessionToken.builder()
                    .id(UUID.randomUUID())
                    .expiration(
                        Date.from(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC)))
                    .build()));

    String renewed = jwtTokenService.renew(token, "test");

    assertNotEquals(renewed, token);

    verify(userService, times(1)).loadUserByUsername(user.getUsername());
    verify(sessionTokenKeyLocator, times(2)).locate(any(ProtectedHeader.class));
    verify(sessionTokenRepository, times(2)).getReferenceById(any(UUID.class));
    verify(sessionTokenRepository, times(2)).save(any(SessionToken.class));
    verify(sessionTokenRepository, times(1)).findAllByParentToken(any(SessionToken.class));
    verifyNoMoreInteractions(sessionTokenKeyLocator);
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void renewFailTest() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();
    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.ACCESS)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    String token =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionToken.getType().name())
            .and()
            .subject(sessionToken.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(jwtConfigProperties.getIssuer())
            .claim(CLAIM_NONCE, user.getNonce())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .signWith(sessionToken.getKey())
            .compact();

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(userService.loadUserByUsername(user.getUsername())).thenReturn(user);

    String message =
        assertThrows(IllegalArgumentException.class, () -> jwtTokenService.renew(token, "test"))
            .getMessage();
    assertEquals("Session token is not a refresh token", message);

    verify(userService, times(1)).loadUserByUsername(user.getUsername());
    verify(sessionTokenKeyLocator, times(1)).locate(any(ProtectedHeader.class));
    verify(sessionTokenRepository, times(1)).getReferenceById(any(UUID.class));
    verifyNoMoreInteractions(sessionTokenKeyLocator);
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void getTokensTest() {
    assertDoesNotThrow(() -> jwtTokenService.getTokens("test@example.com"));
    verify(sessionTokenRepository, times(1))
        .findAllByUsernameAndType("test@example.com", SessionTokenType.REFRESH);
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void deleteTokenTest() {
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(mock(SecretKey.class))
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(new Date())
            .expiration(new Date())
            .userAgent("test")
            .accessTokens(List.of(SessionToken.builder().id(UUID.randomUUID()).build()))
            .build();

    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(sessionTokenRepository.findAllByParentToken(sessionToken))
        .thenReturn(sessionToken.getAccessTokens());

    assertDoesNotThrow(
        () -> jwtTokenService.deleteToken(sessionToken.getUsername(), sessionToken.getId()));
    verify(sessionTokenRepository, times(1)).getReferenceById(sessionToken.getId());
    verify(sessionTokenRepository, times(1)).findAllByParentToken(sessionToken);
    verify(sessionTokenRepository, times(1)).deleteAll(sessionToken.getAccessTokens());
    verify(sessionTokenRepository, times(1)).delete(sessionToken);
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void deleteTokenFailTest() {
    UUID uuid = UUID.randomUUID();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(uuid)
            .key(mock(SecretKey.class))
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(new Date())
            .expiration(new Date())
            .userAgent("test")
            .accessTokens(List.of(SessionToken.builder().id(UUID.randomUUID()).build()))
            .build();

    when(sessionTokenRepository.getReferenceById(uuid)).thenReturn(sessionToken);

    String message =
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenService.deleteToken("other@example.com", uuid))
            .getMessage();
    assertEquals("Session token does not belong to user", message);
    verify(sessionTokenRepository, times(1)).getReferenceById(sessionToken.getId());
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void cleanupTest() {
    assertDoesNotThrow(() -> jwtTokenService.cleanup());
    verify(sessionTokenRepository, times(1)).deleteAllByExpirationBefore(any(Date.class));
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void isAccessTokenValidTrueTest() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();
    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    String refreshToken =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionToken.getType().name())
            .and()
            .subject(sessionToken.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(jwtConfigProperties.getIssuer())
            .claim(CLAIM_NONCE, user.getNonce())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .signWith(sessionToken.getKey())
            .compact();
    final SessionToken[] accessSessionToken = new SessionToken[1];

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(sessionTokenRepository.findAllByParentToken(sessionToken)).thenReturn(List.of());
    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              accessSessionToken[0] = invocation.getArgument(0);
              accessSessionToken[0].setId(UUID.randomUUID());
              return accessSessionToken[0];
            });

    String accessToken =
        jwtTokenService.createToken(user, SessionTokenType.ACCESS, null, refreshToken);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessSessionToken[0].getKey());
    when(sessionTokenRepository.getReferenceById(accessSessionToken[0].getId()))
        .thenReturn(accessSessionToken[0]);

    assertTrue(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }

  @Test
  void isAccessTokenValidFalseTest() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();
    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    SessionToken otherSessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    String refreshToken =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionToken.getType().name())
            .and()
            .subject(sessionToken.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(jwtConfigProperties.getIssuer())
            .claim(CLAIM_NONCE, user.getNonce())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .signWith(sessionToken.getKey())
            .compact();
    final SessionToken[] accessSessionToken = new SessionToken[1];

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(sessionTokenRepository.findAllByParentToken(sessionToken)).thenReturn(List.of());
    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              accessSessionToken[0] = invocation.getArgument(0);
              accessSessionToken[0].setId(UUID.randomUUID());
              accessSessionToken[0].setParentToken(otherSessionToken);
              return accessSessionToken[0];
            });

    String accessToken =
        jwtTokenService.createToken(user, SessionTokenType.ACCESS, null, refreshToken);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessSessionToken[0].getKey());
    when(sessionTokenRepository.getReferenceById(accessSessionToken[0].getId()))
        .thenReturn(accessSessionToken[0]);

    assertFalse(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }

  @Test
  void isAccessTokenValidNoKeyPersistent() {
    TestLongUser user =
        TestLongUser.builder()
            .id(1L)
            .email(RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.randomAlphabetic(5, 10))
            .lastName(RandomStringUtils.randomAlphabetic(5, 10))
            .nonce(RandomStringUtils.randomAlphanumeric(5, 10))
            .build();
    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();
    String refreshToken =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionToken.getType().name())
            .and()
            .subject(sessionToken.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(jwtConfigProperties.getIssuer())
            .claim(CLAIM_NONCE, user.getNonce())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .signWith(sessionToken.getKey())
            .compact();
    final SecretKey[] accessTokenSecretKey = new SecretKey[1];

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
    when(sessionTokenRepository.findAllByParentToken(sessionToken)).thenReturn(List.of());
    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken argument = invocation.getArgument(0);
              argument.setId(UUID.randomUUID());
              accessTokenSecretKey[0] = argument.getKey();
              return argument;
            });

    String accessToken =
        jwtTokenService.createToken(user, SessionTokenType.ACCESS, null, refreshToken);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessTokenSecretKey[0]);
    when(sessionTokenRepository.getReferenceById(any())).thenReturn(null);

    assertFalse(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }
}
