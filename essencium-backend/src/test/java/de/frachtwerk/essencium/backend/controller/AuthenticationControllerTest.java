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

package de.frachtwerk.essencium.backend.controller;

import static de.frachtwerk.essencium.backend.service.JwtTokenService.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.AppProperties;
import de.frachtwerk.essencium.backend.configuration.properties.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.model.dto.TokenResponse;
import de.frachtwerk.essencium.backend.security.JwtTokenAuthenticationFilter;
import de.frachtwerk.essencium.backend.security.event.CustomAuthenticationSuccessEvent;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

  @Mock private AppProperties appPropertiesMock;
  @Mock private AppJwtProperties appConfigJwtPropertiesMock;
  @Mock private JwtTokenService jwtTokenServiceMock;
  @Mock private JwtTokenAuthenticationFilter<Long> jwtTokenAuthenticationFilter;
  @Mock private AuthenticationManager authenticationManagerMock;
  @Mock private ApplicationEventPublisher applicationEventPublisherMock;
  @Mock private OAuth2ClientRegistrationProperties oAuth2ClientRegistrationPropertiesMock;
  @Mock private AppOAuth2Properties appOAuth2PropertiesMock;

  @InjectMocks AuthenticationController authenticationController;

  @Test
  void postLogin() {
    LoginRequest loginRequest =
        new LoginRequest("test@example.com", "verySecurePassword123456789!#");
    String userAgent = "Unit Test";
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
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
            .issuer("Issuer")
            .claim(CLAIM_FIRST_NAME, "testFirstName")
            .claim(CLAIM_LAST_NAME, "testLastName")
            .claim(CLAIM_UID, 1L)
            .signWith(sessionToken.getKey())
            .compact();

    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("test@example.com");

    AbstractBaseUser principal = mock(AbstractBaseUser.class);
    when(authentication.getPrincipal()).thenReturn(principal);
    when(principal.toEssenciumUserDetails())
        .thenReturn(EssenciumUserDetails.builder().id(1L).username("test@example.com").build());

    when(authenticationManagerMock.authenticate(any())).thenReturn(authentication);
    doNothing()
        .when(applicationEventPublisherMock)
        .publishEvent(any(CustomAuthenticationSuccessEvent.class));
    when(jwtTokenServiceMock.login(any(), anyString())).thenReturn(token);
    when(appConfigJwtPropertiesMock.getRefreshTokenExpiration()).thenReturn(86400);
    when(appPropertiesMock.getDomain()).thenReturn("example.com");
    when(jwtTokenServiceMock.renew(token, userAgent)).thenReturn(token);

    TokenResponse tokenResponse =
        authenticationController.postLogin(loginRequest, userAgent, httpServletResponse);

    assertNotNull(tokenResponse);
    assertNotNull(tokenResponse.token());
    assertEquals(token, tokenResponse.token());
    verify(httpServletResponse, times(1)).addCookie(any());
    verify(authenticationManagerMock, times(1)).authenticate(any());
    verify(applicationEventPublisherMock, times(1))
        .publishEvent(any(CustomAuthenticationSuccessEvent.class));
    verify(jwtTokenServiceMock, times(1)).login(any(), anyString());
    verify(appConfigJwtPropertiesMock, times(1)).getRefreshTokenExpiration();
    verify(appPropertiesMock, times(1)).getDomain();
    verify(jwtTokenServiceMock, times(1)).renew(token, userAgent);
    verifyNoMoreInteractions(authenticationManagerMock);
    verifyNoMoreInteractions(applicationEventPublisherMock);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoMoreInteractions(appConfigJwtPropertiesMock);
  }

  @Test
  void postLoginFail() {
    LoginRequest loginRequest =
        new LoginRequest("test@example.com", "verySecurePassword123456789!#");
    String userAgent = "Unit Test";
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

    when(authenticationManagerMock.authenticate(any())).thenThrow(BadCredentialsException.class);

    ResponseStatusException responseStatusException =
        assertThrows(
            ResponseStatusException.class,
            () -> authenticationController.postLogin(loginRequest, userAgent, httpServletResponse));
    assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatusCode());
    verify(authenticationManagerMock, times(1)).authenticate(any());
    verifyNoMoreInteractions(authenticationManagerMock);
    verifyNoMoreInteractions(applicationEventPublisherMock);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoMoreInteractions(appConfigJwtPropertiesMock);
  }

  @Test
  void postRenew() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
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
            .issuer("Issuer")
            .claim(CLAIM_FIRST_NAME, "testFirstName")
            .claim(CLAIM_LAST_NAME, "testLastName")
            .claim(CLAIM_UID, 1L)
            .signWith(sessionToken.getKey())
            .compact();

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(jwtTokenAuthenticationFilter.getAuthentication(token)).thenReturn(authentication);
    when(jwtTokenServiceMock.isAccessTokenValid(anyString(), anyString())).thenReturn(true);
    when(jwtTokenServiceMock.renew(token, userAgent)).thenReturn(token);

    TokenResponse tokenResponse =
        authenticationController.postRenew(userAgent, token, httpServletRequest);

    assertNotNull(tokenResponse);
    assertNotNull(tokenResponse.token());
    assertEquals(token, tokenResponse.token());
    verify(jwtTokenServiceMock, times(1)).renew(token, userAgent);
    verifyNoMoreInteractions(jwtTokenServiceMock);
  }

  @Test
  void postRenewFail() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent(userAgent)
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
            .issuer("Issuer")
            .claim(CLAIM_FIRST_NAME, "testFirstName")
            .claim(CLAIM_LAST_NAME, "testLastName")
            .claim(CLAIM_UID, 1L)
            .signWith(sessionToken.getKey())
            .compact();

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(jwtTokenAuthenticationFilter.getAuthentication(token)).thenReturn(authentication);
    when(jwtTokenServiceMock.renew(token, userAgent)).thenThrow(BadCredentialsException.class);
    when(jwtTokenServiceMock.isAccessTokenValid(anyString(), anyString())).thenReturn(true);

    BadCredentialsException badCredentialsException =
        assertThrows(
            BadCredentialsException.class,
            () -> authenticationController.postRenew(userAgent, token, httpServletRequest));

    verify(jwtTokenServiceMock, times(1)).renew(anyString(), anyString());
    verifyNoMoreInteractions(authenticationManagerMock);
    verifyNoMoreInteractions(applicationEventPublisherMock);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoMoreInteractions(appConfigJwtPropertiesMock);
  }

  @Test
  void postRenewFailInvalidSessionToken() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent(userAgent)
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
            .issuer("Issuer")
            .claim(CLAIM_FIRST_NAME, "testFirstName")
            .claim(CLAIM_LAST_NAME, "testLastName")
            .claim(CLAIM_UID, 1L)
            .signWith(sessionToken.getKey())
            .compact();

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(false);
    when(jwtTokenAuthenticationFilter.getAuthentication(token)).thenReturn(authentication);

    ResponseStatusException responseStatusException =
        assertThrows(
            ResponseStatusException.class,
            () -> authenticationController.postRenew(userAgent, token, httpServletRequest));

    assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatusCode());

    assertEquals("Refresh token is invalid", responseStatusException.getReason());
    verifyNoMoreInteractions(jwtTokenAuthenticationFilter);
    verifyNoMoreInteractions(authenticationManagerMock);
    verifyNoMoreInteractions(applicationEventPublisherMock);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoMoreInteractions(appConfigJwtPropertiesMock);
  }

  @Test
  void renewInvalidAccessToken() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent(userAgent)
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
            .issuer("Issuer")
            .claim(CLAIM_FIRST_NAME, "testFirstName")
            .claim(CLAIM_LAST_NAME, "testLastName")
            .claim(CLAIM_UID, 1L)
            .signWith(sessionToken.getKey())
            .compact();

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(jwtTokenAuthenticationFilter.getAuthentication(token)).thenReturn(authentication);
    when(jwtTokenServiceMock.isAccessTokenValid(anyString(), anyString())).thenReturn(false);

    ResponseStatusException responseStatusException =
        assertThrows(
            ResponseStatusException.class,
            () -> authenticationController.postRenew(userAgent, token, httpServletRequest));

    assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatusCode());
    assertEquals(
        "Refresh token and access token do not belong together",
        responseStatusException.getReason());
    verifyNoMoreInteractions(jwtTokenAuthenticationFilter);
    verifyNoMoreInteractions(authenticationManagerMock);
    verifyNoMoreInteractions(applicationEventPublisherMock);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoMoreInteractions(appConfigJwtPropertiesMock);
  }

  @Test
  void collectionOptions() {
    ResponseEntity<?> responseEntity = authenticationController.collectionOptions();

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(
        responseEntity.getHeaders().getAllow(), authenticationController.getAllowedMethods());
  }

  @Test
  void getRegistrationsOauthDisabled() {
    when(appOAuth2PropertiesMock.isEnabled()).thenReturn(false);

    Map<String, Map<String, Object>> registrations = authenticationController.getRegistrations();

    assertNotNull(registrations);
    assertTrue(registrations.isEmpty());
    verifyNoMoreInteractions(oAuth2ClientRegistrationPropertiesMock, appOAuth2PropertiesMock);
  }

  @Test
  void getRegistrationsEmpty() {
    when(oAuth2ClientRegistrationPropertiesMock.getRegistration()).thenReturn(Map.of());
    when(appOAuth2PropertiesMock.isEnabled()).thenReturn(true);

    Map<String, Map<String, Object>> registrations = authenticationController.getRegistrations();

    assertNotNull(registrations);
    assertTrue(registrations.isEmpty());
    verify(oAuth2ClientRegistrationPropertiesMock, times(2)).getRegistration();
    verifyNoMoreInteractions(oAuth2ClientRegistrationPropertiesMock, appOAuth2PropertiesMock);
  }

  @Test
  void getRegistrationsWithProvider() {
    Map<String, OAuth2ClientRegistrationProperties.Registration> registrationMap = new HashMap<>();
    registrationMap.put(
        "test",
        OAuth2ClientRegistrationProperties.Registration.builder()
            .clientName("Test")
            .imageUrl("https://example.com/test.png")
            .build());

    Map<String, OAuth2ClientRegistrationProperties.ClientProvider> providerMap = new HashMap<>();
    providerMap.put(
        "test",
        OAuth2ClientRegistrationProperties.ClientProvider.builder()
            .allowSignup(true)
            .updateRole(true)
            .build());

    when(oAuth2ClientRegistrationPropertiesMock.getProvider()).thenReturn(providerMap);
    when(oAuth2ClientRegistrationPropertiesMock.getRegistration()).thenReturn(registrationMap);
    when(appOAuth2PropertiesMock.isEnabled()).thenReturn(true);

    Map<String, Map<String, Object>> registrations = authenticationController.getRegistrations();

    assertNotNull(registrations);
    assertThat(registrations.size()).isOne();
    assertTrue(registrations.containsKey("test"));

    Map<String, Object> providerRegistration = registrations.get("test");
    assertInstanceOf(Map.class, providerRegistration);

    assertThat(providerRegistration).hasSize(5);
    assertTrue(providerRegistration.keySet().containsAll(List.of("name", "url", "imageUrl")));
    assertEquals("Test", providerRegistration.get("name"));
    assertEquals("/oauth2/authorization/test", providerRegistration.get("url"));
    assertEquals("https://example.com/test.png", providerRegistration.get("imageUrl"));
    assertEquals(true, providerRegistration.get("allowSignup"));
    assertEquals(true, providerRegistration.get("updateRole"));

    verify(oAuth2ClientRegistrationPropertiesMock, times(2)).getRegistration();
    verifyNoMoreInteractions(oAuth2ClientRegistrationPropertiesMock, appOAuth2PropertiesMock);
  }

  @Nested
  @DisplayName("logout")
  class LogoutTests {
    private final String defaultLogoutRedirectUrl = "https://example.com/default";
    private final List<String> allowedLogoutRedirectUrls =
        List.of(
            "https://example.com/default",
            "https://example.com/home",
            "https://example.com/dashboard",
            "https://example.com/logout",
            "https://regex.com/*",
            "https://*.subregex.com/*");

    private final String bearerToken = "Bearer token";

    public static Stream<Arguments> redirectUrls() {
      return Stream.of(
          Arguments.of("https://regex.com/home", true),
          Arguments.of("https://example.com/home", true),
          Arguments.of("https://example.com/dashboard", true),
          Arguments.of("https://example.com/logout", true),
          Arguments.of("https://example.com/logout/additional", false),
          Arguments.of("https://example.com/unknown", false),
          Arguments.of("https://regex.com/anything", true),
          Arguments.of("https://regex.com/something/else", true),
          Arguments.of("http://regex.com/home", false),
          Arguments.of("http://example.com/home", false),
          Arguments.of("http://example.com/dashboard", false),
          Arguments.of("http://example.com/logout", false),
          Arguments.of("http://example.com/unknown", false),
          Arguments.of("http://regex.com/anything", false),
          Arguments.of("http://regex.com/something/else", false),
          Arguments.of("https://invalid-url.com/home", false),
          Arguments.of("https://invalid-url.com/dashboard", false),
          Arguments.of("https://invalid-url.com/logout", false),
          Arguments.of("https://invalid-url.com", false),
          Arguments.of("https://subregex.com/home", false),
          Arguments.of("https://sub.subregex.com/home", true),
          Arguments.of("https://subregex.com/", false),
          Arguments.of("https://sub.subregex.com/", true));
    }

    @BeforeEach
    void setUp() {
      reset(appPropertiesMock, jwtTokenServiceMock, oAuth2ClientRegistrationPropertiesMock);
    }

    @Test
    void logout_usesDefaultRedirectUrl_whenRedirectUrlIsNull() {
      when(appPropertiesMock.getDefaultLogoutRedirectUrl()).thenReturn(defaultLogoutRedirectUrl);
      when(appPropertiesMock.getAllowedLogoutRedirectUrls()).thenReturn(allowedLogoutRedirectUrls);

      HttpServletResponse response = mock(HttpServletResponse.class);

      authenticationController.logout(bearerToken, null, response);

      verify(jwtTokenServiceMock)
          .logout(
              bearerToken,
              URI.create(defaultLogoutRedirectUrl),
              oAuth2ClientRegistrationPropertiesMock,
              response);
    }

    @Test
    void logout_usesDefaultRedirectUrl_whenRedirectUrlIsBlank() {
      when(appPropertiesMock.getDefaultLogoutRedirectUrl()).thenReturn(defaultLogoutRedirectUrl);
      when(appPropertiesMock.getAllowedLogoutRedirectUrls()).thenReturn(allowedLogoutRedirectUrls);

      HttpServletResponse response = mock(HttpServletResponse.class);

      authenticationController.logout(bearerToken, "", response);

      verify(jwtTokenServiceMock)
          .logout(
              bearerToken,
              URI.create(defaultLogoutRedirectUrl),
              oAuth2ClientRegistrationPropertiesMock,
              response);
    }

    @ParameterizedTest
    @MethodSource("redirectUrls")
    void logout_redirectsToAllowedUrl(String redirectUrl, boolean valid) {
      when(appPropertiesMock.getAllowedLogoutRedirectUrls()).thenReturn(allowedLogoutRedirectUrls);

      HttpServletResponse response = mock(HttpServletResponse.class);

      if (valid) {
        authenticationController.logout(bearerToken, redirectUrl, response);
        verify(jwtTokenServiceMock)
            .logout(
                bearerToken,
                URI.create(redirectUrl),
                oAuth2ClientRegistrationPropertiesMock,
                response);
      } else {
        authenticationController.logout(bearerToken, redirectUrl, response);
        verify(jwtTokenServiceMock)
            .logout(bearerToken, null, oAuth2ClientRegistrationPropertiesMock, response);
      }
    }

    @Test
    void logout_redirectUrlIsNotValidURI() throws IOException {
      HttpServletResponse response = mock(HttpServletResponse.class);

      authenticationController.logout(bearerToken, "invalid url", response);
      verify(jwtTokenServiceMock)
          .logout(bearerToken, null, oAuth2ClientRegistrationPropertiesMock, response);
    }
  }
}
