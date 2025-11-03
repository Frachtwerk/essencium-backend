/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.configuration.properties.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.security.SessionTokenKeyLocator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

  @Mock SessionTokenRepository sessionTokenRepository;
  @Mock SessionTokenKeyLocator sessionTokenKeyLocator;
  AppJwtProperties appConfigJwtProperties;
  @Mock UserMailService userMailService;
  @Mock UserServiceStub userService;
  JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() {
    appConfigJwtProperties = new AppJwtProperties();
    appConfigJwtProperties.setIssuer(RandomStringUtils.secure().nextAlphabetic(5, 10));
    appConfigJwtProperties.setAccessTokenExpiration(86400);
    appConfigJwtProperties.setRefreshTokenExpiration(2592000);
    appConfigJwtProperties.setMaxSessionExpirationTime(2592000);
    appConfigJwtProperties.setCleanupInterval(3600);
    jwtTokenService =
        new JwtTokenService(
            sessionTokenRepository,
            sessionTokenKeyLocator,
            appConfigJwtProperties,
            userMailService);
    jwtTokenService.setUserService(userService);
  }

  @Test
  void loginTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
            .build();

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });

    String token = jwtTokenService.login(user.toEssenciumUserDetails(), "test");

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verify(userMailService, times(1))
        .sendLoginMail(eq(user.getEmail()), any(TokenRepresentation.class), eq(user.getLocale()));
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
  }

  @Test
  void createTokenAccessTest() {
    UserStub user =
        UserStub.builder()
            .id(42L)
            .email(RandomStringUtils.secureStrong().nextAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secureStrong().nextAlphanumeric(5, 10))
            .lastName(RandomStringUtils.secureStrong().nextAlphanumeric(5, 10))
            .locale(Locale.ENGLISH)
            .build();

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });
    String token =
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, "test", null, null);

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verifyNoInteractions(userMailService); // No email for access tokens
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
  }

  @Test
  void createTokenRefreshTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.FRENCH)
            .build();

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });

    String token =
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.REFRESH, "test", null, null);

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verify(userMailService, times(1))
        .sendLoginMail(eq(user.getEmail()), any(TokenRepresentation.class), eq(user.getLocale()));
    verifyNoMoreInteractions(sessionTokenRepository);
    assertNotNull(token);
    assertNotEquals("", token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
  }

  @Test
  void verifyTokenTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
            .build();

    final SessionToken[] sessionToken = {null};

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              sessionToken[0] = invocation.getArgument(0);
              sessionToken[0].setId(UUID.randomUUID());
              return sessionToken[0];
            });

    String token =
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.REFRESH, "test", null, null);

    verify(sessionTokenRepository, times(1)).save(any(SessionToken.class));
    verify(userMailService, times(1)).sendLoginMail(any(), any(), any());
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

    assertThat(claims.getIssuer(), Matchers.is(appConfigJwtProperties.getIssuer()));
    assertThat(claims.getSubject(), Matchers.is(user.getUsername()));
    assertThat(claims.get("given_name", String.class), Matchers.is(user.getFirstName()));
    assertThat(claims.get("family_name", String.class), Matchers.is(user.getLastName()));
    assertThat(claims.get("uid", Long.class), Matchers.is(user.getId()));
    assertThat(claims.get("locale", String.class), Matchers.is(user.getLocale().toString()));
    assertThat(claims.get("custom_claim", String.class), Matchers.is("test_value"));
    assertThat(
        Duration.between(issuedAt.toInstant(), Instant.now()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.greaterThan(0), Matchers.lessThan(5 * 1000 * 1000) // no older than 5 seconds
            ));

    assertThat(
        Duration.between(Instant.now(), expiresAt.toInstant()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.lessThan(appConfigJwtProperties.getAccessTokenExpiration() * 1000 * 1000),
            Matchers.greaterThan(
                appConfigJwtProperties.getAccessTokenExpiration() - 5 * 1000 * 1000),
            Matchers.greaterThan(0)));
  }

  @Test
  void renewTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
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
            .issuer(appConfigJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_LOCALE, user.getLocale().toString())
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
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
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
            .issuer(appConfigJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_LOCALE, user.getLocale().toString())
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
  void isAccessTokenValidTrueTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphanumeric(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphanumeric(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphanumeric(5, 10))
            .locale(Locale.GERMAN)
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
            .issuer(appConfigJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_LOCALE, user.getLocale().toString())
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
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, null, refreshToken, null);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessSessionToken[0].getKey());
    when(sessionTokenRepository.getReferenceById(accessSessionToken[0].getId()))
        .thenReturn(accessSessionToken[0]);

    assertTrue(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }

  @Test
  void isAccessTokenValidFalseTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
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
            .issuer(appConfigJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_LOCALE, user.getLocale().toString())
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
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, null, refreshToken, null);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessSessionToken[0].getKey());
    when(sessionTokenRepository.getReferenceById(accessSessionToken[0].getId()))
        .thenReturn(accessSessionToken[0]);

    assertFalse(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }

  @Test
  void isAccessTokenValidNoKeyPersistent() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
            .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
            .locale(Locale.GERMAN)
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
            .issuer(appConfigJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_LOCALE, user.getLocale().toString())
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
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, null, refreshToken, null);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(secretKey)
        .thenReturn(accessTokenSecretKey[0]);
    when(sessionTokenRepository.getReferenceById(any())).thenReturn(null);

    assertFalse(jwtTokenService.isAccessTokenValid(refreshToken, accessToken));
  }

  @Nested
  @DisplayName("Logout Tests")
  class LogoutTests {
    @Test
    @DisplayName("Null authorizationHeader does not throw an exception")
    void logoutWithNullAuthorizationHeader() {
      assertThrows(
          AuthenticationCredentialsNotFoundException.class,
          () ->
              jwtTokenService.logout(
                  null,
                  URI.create("http://localhost"),
                  mock(OAuth2ClientRegistrationProperties.class),
                  mock(HttpServletResponse.class)));
    }

    @Test
    @DisplayName("Logout with null redirect URI should not throw an exception and return 204")
    void logoutWithNullRedirectUri() {
      SecretKey secretKey = Jwts.SIG.HS512.key().build();
      UserStub user =
          UserStub.builder()
              .id(1L)
              .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
              .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .build();
      SessionToken sessionToken =
          SessionToken.builder()
              .id(UUID.randomUUID())
              .key(secretKey)
              .username(user.getUsername())
              .type(SessionTokenType.ACCESS)
              .build();
      String token =
          Jwts.builder()
              .header()
              .keyId(String.valueOf(sessionToken.getId()))
              .type(SessionTokenType.ACCESS.name())
              .and()
              .issuer(appConfigJwtProperties.getIssuer())
              .subject(user.getUsername())
              .signWith(secretKey)
              .claim(CLAIM_UID, 42L)
              .compact();
      when(sessionTokenKeyLocator.locate(any())).thenReturn(secretKey);
      when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
      when(userService.loadUserByUsername(any())).thenReturn(user);

      HttpServletResponse response = mock(HttpServletResponse.class);

      assertDoesNotThrow(
          () ->
              jwtTokenService.logout(
                  "Bearer " + token,
                  null,
                  mock(OAuth2ClientRegistrationProperties.class),
                  response));
      verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    @DisplayName("Logout with error on setting redirect URI should not throw an exception")
    void logoutWithInvalidRedirectUri() {
      SecretKey secretKey = Jwts.SIG.HS512.key().build();
      UserStub user =
          UserStub.builder()
              .id(1L)
              .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
              .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .build();
      SessionToken sessionToken =
          SessionToken.builder()
              .id(UUID.randomUUID())
              .key(secretKey)
              .username(user.getUsername())
              .type(SessionTokenType.ACCESS)
              .build();
      String token =
          Jwts.builder()
              .header()
              .keyId(String.valueOf(sessionToken.getId()))
              .type(SessionTokenType.ACCESS.name())
              .and()
              .issuer(appConfigJwtProperties.getIssuer())
              .subject(user.getUsername())
              .signWith(secretKey)
              .claim(CLAIM_UID, 42L)
              .compact();
      when(sessionTokenKeyLocator.locate(any())).thenReturn(secretKey);
      when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
      when(userService.loadUserByUsername(any())).thenReturn(user);

      HttpServletResponse response = mock(HttpServletResponse.class);

      // Simulate an error when setting the redirect URI

      try {
        doThrow(new IOException("Invalid redirect URI")).when(response).sendRedirect(anyString());
      } catch (IOException e) {
        fail();
      }

      assertDoesNotThrow(
          () ->
              jwtTokenService.logout(
                  "Bearer " + token,
                  URI.create("http://localhost"),
                  mock(OAuth2ClientRegistrationProperties.class),
                  response));
      verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    @DisplayName("Calling logout should delete the session token")
    void logoutDeletesSessionToken() throws IOException {
      SecretKey secretKey = Jwts.SIG.HS512.key().build();
      UserStub user =
          UserStub.builder()
              .id(1L)
              .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
              .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .build();
      SessionToken refreshToken =
          SessionToken.builder()
              .id(UUID.randomUUID())
              .key(secretKey)
              .username(user.getUsername())
              .type(SessionTokenType.REFRESH)
              .build();
      SessionToken sessionToken =
          SessionToken.builder()
              .id(UUID.randomUUID())
              .key(secretKey)
              .username(user.getUsername())
              .type(SessionTokenType.ACCESS)
              .parentToken(refreshToken)
              .build();
      String token =
          Jwts.builder()
              .header()
              .keyId(String.valueOf(sessionToken.getId()))
              .type(SessionTokenType.ACCESS.name())
              .and()
              .issuer(appConfigJwtProperties.getIssuer())
              .subject(user.getUsername())
              .signWith(secretKey)
              .claim(CLAIM_UID, 42L)
              .compact();
      HttpServletResponse response = mock(HttpServletResponse.class);

      when(sessionTokenKeyLocator.locate(any())).thenReturn(secretKey);
      when(sessionTokenRepository.getReferenceById(refreshToken.getId())).thenReturn(refreshToken);
      when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
      when(userService.loadUserByUsername(any())).thenReturn(user);

      assertDoesNotThrow(
          () ->
              jwtTokenService.logout(
                  "Bearer " + token,
                  URI.create("http://localhost"),
                  mock(OAuth2ClientRegistrationProperties.class),
                  response));

      // 1. getRequestingToken
      // 2. deleteToken
      verify(sessionTokenRepository, times(2)).getReferenceById(any());
      verify(userService, times(1)).loadUserByUsername(any());
      verify(sessionTokenRepository, times(1)).delete(refreshToken);
      verify(response, times(1)).sendRedirect("http://localhost");
    }

    @Test
    @DisplayName(
        "Logout of an oauth user should delete the session token and redirect to the logout URL")
    void logoutOauthUser() throws IOException {
      SecretKey secretKey = Jwts.SIG.HS512.key().build();
      UserStub user =
          UserStub.builder()
              .id(1L)
              .email(RandomStringUtils.secure().nextAlphabetic(5, 10) + "@frachtwerk.de")
              .firstName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .lastName(RandomStringUtils.secure().nextAlphabetic(5, 10))
              .source("oauth2")
              .build();
      SessionToken sessionToken =
          SessionToken.builder()
              .id(UUID.randomUUID())
              .key(secretKey)
              .username(user.getUsername())
              .type(SessionTokenType.ACCESS)
              .build();
      String token =
          Jwts.builder()
              .header()
              .keyId(String.valueOf(sessionToken.getId()))
              .type(SessionTokenType.ACCESS.name())
              .and()
              .issuer(appConfigJwtProperties.getIssuer())
              .subject(user.getUsername())
              .signWith(secretKey)
              .claim(CLAIM_UID, 42L)
              .compact();
      HttpServletResponse response = mock(HttpServletResponse.class);
      OAuth2ClientRegistrationProperties oAuth2ClientRegistrationProperties =
          mock(OAuth2ClientRegistrationProperties.class);
      OAuth2ClientRegistrationProperties.Registration clientRegistration =
          mock(OAuth2ClientRegistrationProperties.Registration.class);
      OAuth2ClientRegistrationProperties.ClientProvider clientProvider =
          mock(OAuth2ClientRegistrationProperties.ClientProvider.class);

      when(sessionTokenKeyLocator.locate(any())).thenReturn(secretKey);
      when(sessionTokenRepository.getReferenceById(sessionToken.getId())).thenReturn(sessionToken);
      when(userService.loadUserByUsername(any())).thenReturn(user);
      when(oAuth2ClientRegistrationProperties.getRegistration())
          .thenReturn(Map.of("oauth2", clientRegistration));
      when(clientRegistration.getProvider()).thenReturn("oauth2-provider");
      when(oAuth2ClientRegistrationProperties.getProvider())
          .thenReturn(Map.of("oauth2-provider", clientProvider));
      when(clientProvider.getLogoutUri()).thenReturn("http://auth-provider/logout");

      assertDoesNotThrow(
          () ->
              jwtTokenService.logout(
                  "Bearer " + token,
                  URI.create("http://localhost"),
                  oAuth2ClientRegistrationProperties,
                  response));

      // 1. getRequestingToken
      // 2. deleteToken
      verify(sessionTokenRepository, times(2)).getReferenceById(any());
      verify(userService, times(1)).loadUserByUsername(any());
      verify(sessionTokenRepository, times(1)).delete(sessionToken);
      verify(response, times(1)).sendRedirect("http://auth-provider/logout");
    }
  }

  @Test
  void deleteAllbyUsernameEqualsIgnoreCaseTest() {
    String username = "Test@Example.Com";

    assertDoesNotThrow(() -> jwtTokenService.deleteAllbyUsernameEqualsIgnoreCase(username));

    verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(username);
    verifyNoMoreInteractions(sessionTokenRepository);
  }

  @Test
  void loginTestWithEmailVerification() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email("test@frachtwerk.de")
            .firstName("John")
            .lastName("Doe")
            .locale(Locale.GERMAN)
            .build();

    ArgumentCaptor<TokenRepresentation> tokenCaptor =
        ArgumentCaptor.forClass(TokenRepresentation.class);

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });

    String token = jwtTokenService.login(user.toEssenciumUserDetails(), "Mozilla/5.0");

    verify(userMailService, times(1))
        .sendLoginMail(eq("test@frachtwerk.de"), tokenCaptor.capture(), eq(Locale.GERMAN));

    TokenRepresentation capturedToken = tokenCaptor.getValue();
    assertNotNull(capturedToken.getId());
    assertEquals(SessionTokenType.REFRESH, capturedToken.getType());
    assertNotNull(capturedToken.getIssuedAt());
    assertNotNull(capturedToken.getExpiration());
    assertEquals("Mozilla/5.0", capturedToken.getUserAgent());

    assertNotNull(token);
  }

  @Test
  void createTokenWithInvalidationOfExistingAccessTokensTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email("test@frachtwerk.de")
            .firstName("John")
            .lastName("Doe")
            .locale(Locale.GERMAN)
            .build();

    SecretKey secretKey = Jwts.SIG.HS512.key().build();
    LocalDateTime now = LocalDateTime.now();

    SessionToken refreshToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(secretKey)
            .username(user.getUsername())
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .build();

    SessionToken existingAccessToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username(user.getUsername())
            .type(SessionTokenType.ACCESS)
            .issuedAt(Date.from(now.minusHours(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusHours(1).toInstant(ZoneOffset.UTC)))
            .userAgent("test")
            .parentToken(refreshToken)
            .build();

    String bearerToken =
        Jwts.builder()
            .header()
            .keyId(refreshToken.getId().toString())
            .type(refreshToken.getType().name())
            .and()
            .subject(refreshToken.getUsername())
            .issuedAt(refreshToken.getIssuedAt())
            .expiration(refreshToken.getExpiration())
            .issuer(appConfigJwtProperties.getIssuer())
            .signWith(refreshToken.getKey())
            .compact();

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class))).thenReturn(secretKey);
    when(sessionTokenRepository.getReferenceById(refreshToken.getId())).thenReturn(refreshToken);
    when(sessionTokenRepository.findAllByParentToken(refreshToken))
        .thenReturn(List.of(existingAccessToken));
    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              SessionToken sessionToken = invocation.getArgument(0);
              sessionToken.setId(UUID.randomUUID());
              return sessionToken;
            });

    String token =
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, "test", bearerToken, null);

    // Verify that existing access token was invalidated (expiration set to now)
    verify(sessionTokenRepository, times(2))
        .save(any(SessionToken.class)); // once for invalidation, once for new token
    assertNotNull(token);
    assertTrue(
        Pattern.matches("^([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_=]+)\\.([a-zA-Z0-9_\\-\\+\\/=]*)", token));
  }

  @Test
  void verifyTokenWithAdditionalClaimsTest() {
    UserStub user =
        UserStub.builder()
            .id(1L)
            .email("test@frachtwerk.de")
            .firstName("John")
            .lastName("Doe")
            .locale(Locale.GERMAN)
            .build();

    final SessionToken[] sessionToken = {null};

    when(sessionTokenRepository.save(any(SessionToken.class)))
        .thenAnswer(
            invocation -> {
              sessionToken[0] = invocation.getArgument(0);
              sessionToken[0].setId(UUID.randomUUID());
              return sessionToken[0];
            });
    String token =
        jwtTokenService.createToken(
            user.toEssenciumUserDetails(), SessionTokenType.ACCESS, "test", null, null);

    when(sessionTokenKeyLocator.locate(any(ProtectedHeader.class)))
        .thenReturn(sessionToken[0].getKey());

    Claims claims = jwtTokenService.verifyToken(token);

    assertThat(claims.get("custom_claim", String.class), Matchers.is("test_value"));
    assertThat(claims.get("locale", String.class), Matchers.is("de"));
  }
}
