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

package de.frachtwerk.essencium.backend.test.integration.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ConfigProperties;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.security.oauth2.OAuth2SuccessHandler;
import de.frachtwerk.essencium.backend.security.oauth2.util.CookieUtil;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.service.TestUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class OAuth2SuccessHandlerTest {
  JwtTokenService tokenServiceMock = mock(JwtTokenService.class);
  TestUserService userServiceMock = mock(TestUserService.class);
  RoleService roleServiceMock = mock(RoleService.class);
  OAuth2ClientRegistrationProperties oAuth2ClientRegistrationPropertiesMock =
      mock(OAuth2ClientRegistrationProperties.class);

  @Test
  void testOnAuthenticationSuccessDoNothingWithoutUserEmail() throws ServletException, IOException {
    OAuth2ConfigProperties oAuth2ConfigProperties = new OAuth2ConfigProperties();

    OAuth2SuccessHandler<TestUser, Long, TestUserDto> oAuth2SuccessHandler =
        new OAuth2SuccessHandler<>(
            tokenServiceMock,
            userServiceMock,
            roleServiceMock,
            oAuth2ConfigProperties,
            oAuth2ClientRegistrationPropertiesMock);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken tokenMock = mock(OAuth2AuthenticationToken.class);

    OidcIdToken idToken =
        OidcIdToken.withTokenValue("test").claim("test", "test").subject("test").build();
    OidcUserInfo userInfo =
        OidcUserInfo.builder()
            .claim("test", "test")
            .subject("test")
            .givenName("Admin")
            .familyName("User")
            .build();
    DefaultOidcUser user = new DefaultOidcUser(null, idToken, userInfo);

    when(tokenMock.getPrincipal()).thenReturn(user);
    when(tokenMock.getName()).thenReturn("test");

    oAuth2SuccessHandler.onAuthenticationSuccess(request, response, tokenMock);

    verifyNoInteractions(
        tokenServiceMock, userServiceMock, roleServiceMock, oAuth2ClientRegistrationPropertiesMock);
  }

  @Test
  void testOnAuthenticationSuccessCreateUserWithGivenAndFamilyName()
      throws ServletException, IOException {
    OAuth2ConfigProperties oAuth2ConfigProperties = new OAuth2ConfigProperties();
    oAuth2ConfigProperties.setAllowSignup(true);
    oAuth2ConfigProperties.setDefaultRedirectUrl("http://localhost:8080");

    OAuth2SuccessHandler<TestUser, Long, TestUserDto> oAuth2SuccessHandler =
        new OAuth2SuccessHandler<>(
            tokenServiceMock,
            userServiceMock,
            roleServiceMock,
            oAuth2ConfigProperties,
            oAuth2ClientRegistrationPropertiesMock);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken tokenMock = mock(OAuth2AuthenticationToken.class);

    OidcIdToken idToken =
        OidcIdToken.withTokenValue("test").claim("test", "test").subject("test").build();
    OidcUserInfo userInfo =
        OidcUserInfo.builder()
            .claim("test", "test")
            .subject("test")
            .givenName("Admin")
            .familyName("User")
            .email("admin.user@test.te")
            .build();
    DefaultOidcUser user = new DefaultOidcUser(null, idToken, userInfo);

    when(tokenMock.getPrincipal()).thenReturn(user);
    when(tokenMock.getName()).thenReturn("test");

    when(userServiceMock.loadUserByUsername(anyString()))
        .thenThrow(new UsernameNotFoundException("e"));

    oAuth2SuccessHandler.onAuthenticationSuccess(request, response, tokenMock);

    verify(userServiceMock, times(1)).loadUserByUsername("admin.user@test.te");

    ArgumentCaptor<UserInfoEssentials> userInfoCaptor =
        ArgumentCaptor.forClass(UserInfoEssentials.class);
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(userServiceMock, times(1))
        .createDefaultUser(userInfoCaptor.capture(), stringCaptor.capture());

    assertEquals(userInfo.getEmail(), userInfoCaptor.getValue().getUsername());
    assertEquals(userInfo.getGivenName(), userInfoCaptor.getValue().getFirstName());
    assertEquals(userInfo.getFamilyName(), userInfoCaptor.getValue().getLastName());

    verify(tokenServiceMock, times(1))
        .createToken(any(), eq(SessionTokenType.ACCESS), eq(null), eq(null));
    verifyNoMoreInteractions(
        tokenServiceMock, userServiceMock, roleServiceMock, oAuth2ClientRegistrationPropertiesMock);
  }

  @Test
  void testOnAuthenticationSuccessCreateUserWithFullNameGiven()
      throws ServletException, IOException {
    OAuth2ConfigProperties oAuth2ConfigProperties = new OAuth2ConfigProperties();
    oAuth2ConfigProperties.setAllowSignup(true);

    OAuth2SuccessHandler<TestUser, Long, TestUserDto> oAuth2SuccessHandler =
        new OAuth2SuccessHandler<>(
            tokenServiceMock,
            userServiceMock,
            roleServiceMock,
            oAuth2ConfigProperties,
            oAuth2ClientRegistrationPropertiesMock);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(CookieUtil.OAUTH2_REQUEST_COOKIE_NAME, "http://localhost:8090"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken tokenMock = mock(OAuth2AuthenticationToken.class);

    OidcIdToken idToken =
        OidcIdToken.withTokenValue("test").claim("test", "test").subject("test").build();
    OidcUserInfo userInfo =
        OidcUserInfo.builder()
            .claim("test", "test")
            .subject("test")
            .name("Admin User")
            .email("admin.user@test.te")
            .build();
    DefaultOidcUser user = new DefaultOidcUser(null, idToken, userInfo);

    when(tokenMock.getPrincipal()).thenReturn(user);
    when(tokenMock.getName()).thenReturn("test");

    when(userServiceMock.loadUserByUsername(anyString()))
        .thenThrow(new UsernameNotFoundException("e"));

    oAuth2SuccessHandler.onAuthenticationSuccess(request, response, tokenMock);

    verify(userServiceMock, times(1)).loadUserByUsername("admin.user@test.te");

    ArgumentCaptor<UserInfoEssentials> userInfoCaptor =
        ArgumentCaptor.forClass(UserInfoEssentials.class);
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(userServiceMock, times(1))
        .createDefaultUser(userInfoCaptor.capture(), stringCaptor.capture());

    assertEquals(userInfo.getEmail(), userInfoCaptor.getValue().getUsername());
    assertEquals("Admin", userInfoCaptor.getValue().getFirstName());
    assertEquals("User", userInfoCaptor.getValue().getLastName());

    verify(tokenServiceMock, times(1))
        .createToken(any(), eq(SessionTokenType.ACCESS), eq(null), eq(null));
    verifyNoMoreInteractions(
        tokenServiceMock, userServiceMock, roleServiceMock, oAuth2ClientRegistrationPropertiesMock);
  }

  @Test
  void testOnAuthenticationSuccessCreateUserWithOutUserInfo() throws ServletException, IOException {
    OAuth2ConfigProperties oAuth2ConfigProperties = new OAuth2ConfigProperties();
    oAuth2ConfigProperties.setAllowSignup(true);

    OAuth2SuccessHandler<TestUser, Long, TestUserDto> oAuth2SuccessHandler =
        new OAuth2SuccessHandler<>(
            tokenServiceMock,
            userServiceMock,
            roleServiceMock,
            oAuth2ConfigProperties,
            oAuth2ClientRegistrationPropertiesMock);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(CookieUtil.OAUTH2_REQUEST_COOKIE_NAME, "http://localhost:8090"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken tokenMock = mock(OAuth2AuthenticationToken.class);

    OidcIdToken idToken =
        OidcIdToken.withTokenValue("test").claim("test", "test").subject("test").build();

    OidcUser user =
        new TestOidcUser(
            idToken,
            null,
            Map.of("given_name", "Admin", "family_name", "User", "email", "admin.user@test.te"));

    when(tokenMock.getPrincipal()).thenReturn(user);
    when(tokenMock.getName()).thenReturn("test");

    when(userServiceMock.loadUserByUsername("admin.user@test.te"))
        .thenThrow(new UsernameNotFoundException("e"));

    oAuth2SuccessHandler.onAuthenticationSuccess(request, response, tokenMock);

    verify(userServiceMock, times(1)).loadUserByUsername("admin.user@test.te");

    ArgumentCaptor<UserInfoEssentials> userInfoCaptor =
        ArgumentCaptor.forClass(UserInfoEssentials.class);
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(userServiceMock, times(1))
        .createDefaultUser(userInfoCaptor.capture(), stringCaptor.capture());

    assertEquals("admin.user@test.te", userInfoCaptor.getValue().getUsername());
    assertEquals("Admin", userInfoCaptor.getValue().getFirstName());
    assertEquals("User", userInfoCaptor.getValue().getLastName());

    verify(tokenServiceMock, times(1))
        .createToken(any(), eq(SessionTokenType.ACCESS), eq(null), eq(null));
    verifyNoMoreInteractions(
        tokenServiceMock, userServiceMock, roleServiceMock, oAuth2ClientRegistrationPropertiesMock);
  }

  private static class TestOidcUser extends DefaultOidcUser {

    private final Map<String, Object> attributes;

    public TestOidcUser(
        OidcIdToken idToken, OidcUserInfo userInfo, Map<String, Object> attributes) {
      super(null, idToken, userInfo);
      this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return this.attributes;
    }
  }
}
