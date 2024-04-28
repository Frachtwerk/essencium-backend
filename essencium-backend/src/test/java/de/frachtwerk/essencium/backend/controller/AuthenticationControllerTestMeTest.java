package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.AppConfigProperties;
import de.frachtwerk.essencium.backend.configuration.properties.JwtConfigProperties;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.model.dto.TokenResponse;
import de.frachtwerk.essencium.backend.security.JwtTokenAuthenticationFilter;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

class AuthenticationControllerTestMeTest {
  @Mock AppConfigProperties appConfigProperties;
  @Mock JwtConfigProperties jwtConfigProperties;
  @Mock JwtTokenService jwtTokenService;
  @Mock JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
  @Mock AuthenticationManager authenticationManager;
  @Mock ApplicationEventPublisher applicationEventPublisher;
  @Mock OAuth2ClientRegistrationProperties oAuth2ClientRegistrationProperties;
  @InjectMocks AuthenticationController authenticationController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetBearerTokenHeader() {
    String result = AuthenticationController.getBearerTokenHeader(null);
    Assertions.assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testPostLogin() {
    when(appConfigProperties.getDomain()).thenReturn("getDomainResponse");
    when(jwtConfigProperties.getRefreshTokenExpiration()).thenReturn(0);
    when(jwtTokenService.login(any(AbstractBaseUser.class), anyString()))
        .thenReturn("loginResponse");
    when(jwtTokenService.renew(anyString(), anyString())).thenReturn("renewResponse");
    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(null);

    TokenResponse result =
        authenticationController.postLogin(
            new LoginRequest("username", "password"), "userAgent", null);
    verify(applicationEventPublisher).publishEvent(any(ApplicationEvent.class));
    Assertions.assertEquals(new TokenResponse("token"), result);
  }

  @Test
  void testPostRenew() {
    when(jwtTokenService.renew(anyString(), anyString())).thenReturn("renewResponse");
    when(jwtTokenService.isAccessTokenValid(anyString(), anyString())).thenReturn(true);
    when(jwtTokenAuthenticationFilter.getAuthentication(anyString())).thenReturn(null);
    when(jwtTokenAuthenticationFilter.extractBearerToken(anyString()))
        .thenReturn("extractBearerTokenResponse");

    TokenResponse result = authenticationController.postRenew("userAgent", "refreshToken", null);
    Assertions.assertEquals(new TokenResponse("token"), result);
  }

  @Test
  void testGetRegistrations() {
    when(oAuth2ClientRegistrationProperties.getRegistration())
        .thenReturn(Map.of("getRegistrationResponse", null));

    Map<String, Object> result = authenticationController.getRegistrations();
    Assertions.assertEquals(
        Map.of("replaceMeWithExpectedResult", "replaceMeWithExpectedResult"), result);
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = authenticationController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = authenticationController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
