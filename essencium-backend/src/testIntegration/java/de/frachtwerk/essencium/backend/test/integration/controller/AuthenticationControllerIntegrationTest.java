/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.test.integration.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer;
import de.frachtwerk.essencium.backend.configuration.properties.*;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.ClientProperties;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.ClientProvider;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.ClientRegistration;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.ClientRegistrationAttributes;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import de.frachtwerk.essencium.backend.test.integration.util.extension.WireMockExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

public class AuthenticationControllerIntegrationTest {

  @Nested
  @SpringBootTest
  @ExtendWith(SpringExtension.class)
  @AutoConfigureMockMvc
  @ActiveProfiles({"local_integration_test"})
  class Local {
    @Autowired private MockMvc mockMvc;
    @Autowired private TestingUtils testingUtils;
    @Autowired private JwtConfigProperties jwtConfigProperties;

    @Test
    void testJwtValid() throws Exception {
      final var randomUser = testingUtils.createRandomUser();
      final var token = testingUtils.createAccessToken(randomUser, mockMvc);
      testValidJwt(token, jwtConfigProperties, randomUser);
    }

    @AfterEach
    public void tearDownSingle() {
      testingUtils.clearUsers();
    }
  }

  @Nested
  @SpringBootTest
  @ExtendWith(SpringExtension.class)
  @AutoConfigureMockMvc
  @ActiveProfiles({"local_integration_test", "with_ldap"})
  class Ldap {

    private static final String TEST_LDAP_NEW_USERNAME = "john.doe@frachtwerk.de";
    private static final String TEST_LDAP_NEW_FIRST_NAME = "John";
    private static final String TEST_LDAP_NEW_LAST_NAME = "Doe";
    private static final String TEST_LDAP_NEW_PASSWORD = "verysecretdontellanyone";
    private static final String TEST_LDAP_NEW_ROLE =
        "ADMIN"; // because of "memberOf: cn=admin,ou=groups,dc=user,dc=frachtwerk,dc=de"

    private static final String TEST_LDAP_EXISTING_USERNAME = "peter.pan@frachtwerk.de";
    private static final String TEST_LDAP_EXISTING_FIRST_NAME = "Peter";
    private static final String TEST_LDAP_EXISTING_LAST_NAME = "Pan";
    private static final String TEST_LDAP_EXISTING_PASSWORD = "verysecretdontellanyone";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestBaseUserRepository userRepository;
    @Autowired private TestingUtils testingUtils;
    @Autowired private LdapConfigProperties ldapConfigProperties;
    @Autowired private RoleService roleService;
    @Autowired private DefaultRoleProperties defaultRoleProperties;

    @BeforeEach
    public void setupSingle() {
      testingUtils.clearUsers();
      testingUtils.createUser(
          TestUserDto.builder()
              .email(TEST_LDAP_EXISTING_USERNAME)
              .firstName(TEST_LDAP_EXISTING_FIRST_NAME)
              .lastName(TEST_LDAP_EXISTING_LAST_NAME)
              .password(TEST_LDAP_EXISTING_PASSWORD)
              .locale(Locale.GERMANY)
              .source(AbstractBaseUser.USER_AUTH_SOURCE_LDAP)
              .role(roleService.getDefaultRole().get().getName())
              .build());
    }

    @AfterEach
    public void tearDownSingle() {
      testingUtils.clearUsers();
      userRepository
          .findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME)
          .ifPresent(user -> userRepository.deleteById(user.getId()));
    }

    @Test
    void testLogin() throws Exception {
      final var loginData =
          new LoginRequest(TEST_LDAP_EXISTING_USERNAME, TEST_LDAP_EXISTING_PASSWORD);

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_EXISTING_USERNAME).isPresent(),
          Matchers.is(true));

      doLogin(loginData);
    }

    @Test
    void testSignup() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getEmail(), Matchers.is(TEST_LDAP_NEW_USERNAME));
      assertThat(newUser.get().getFirstName(), Matchers.is(TEST_LDAP_NEW_FIRST_NAME));
      assertThat(newUser.get().getLastName(), Matchers.is(TEST_LDAP_NEW_LAST_NAME));
      assertThat(newUser.get().getSource(), Matchers.is(AbstractBaseUser.USER_AUTH_SOURCE_LDAP));
      assertThat(newUser.get().getRole().getName(), Matchers.is(TEST_LDAP_NEW_ROLE));
    }

    @Test
    void testSignupDefaultRole() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);

      final var groups =
          ldapConfigProperties.getRoles().stream()
              .map(m -> new UserRoleMapping(m.getSrc(), m.getDst()))
              .collect(Collectors.toList());
      ldapConfigProperties.getRoles().clear();

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));

      ldapConfigProperties.setRoles(groups);
    }

    @Test
    void testSignupNonExistingRole() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);

      final var groups =
          ldapConfigProperties.getRoles().stream()
              .map(m -> new UserRoleMapping(m.getSrc(), m.getDst()))
              .collect(Collectors.toList());
      ldapConfigProperties.setRoles(
          List.of(
              new UserRoleMapping(
                  "cn=admin,ou=groups,dc=user,dc=frachtwerk,dc=de", "NON_EXISTING_GROUP")));

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));

      ldapConfigProperties.setRoles(groups);
    }

    @Test
    void testLoginUpdateRole() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);
      final var groups =
          ldapConfigProperties.getRoles().stream()
              .map(m -> new UserRoleMapping(m.getSrc(), m.getDst()))
              .collect(Collectors.toList());

      ldapConfigProperties.setUpdateRole(true);
      ldapConfigProperties.getRoles().clear();

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));

      ldapConfigProperties.setRoles(groups);

      doLogin(loginData);
      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(
          loggedInUser.get().getRole().getName(),
          Matchers.is(DefaultRoleInitializer.ADMIN_ROLE_NAME));

      assertEquals(newUser.get().getId(), loggedInUser.get().getId());
    }

    @Test
    void testLoginUpdateRoleFallbackToDefault() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);
      final var groups =
          ldapConfigProperties.getRoles().stream()
              .map(m -> new UserRoleMapping(m.getSrc(), m.getDst()))
              .collect(Collectors.toList());

      ldapConfigProperties.setUpdateRole(true);
      ldapConfigProperties.setRoles(groups);

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(
          newUser.get().getRole().getName(), Matchers.is(DefaultRoleInitializer.ADMIN_ROLE_NAME));

      ldapConfigProperties.getRoles().clear();

      doLogin(loginData);
      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(
          loggedInUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));

      assertEquals(newUser.get().getId(), loggedInUser.get().getId());
    }

    @Test
    void testLoginNoUpdateRoleIfDisabled() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);

      ldapConfigProperties.setUpdateRole(false);

      final var groups =
          ldapConfigProperties.getRoles().stream()
              .map(m -> new UserRoleMapping(m.getSrc(), m.getDst()))
              .collect(Collectors.toList());
      ldapConfigProperties.getRoles().clear();

      doLogin(loginData);
      final var newUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));

      ldapConfigProperties.setRoles(groups);

      doLogin(loginData);
      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(
          loggedInUser.get().getRole().getName(), Matchers.is(defaultRoleProperties.getName()));
    }

    @Test
    void testSignupDisabled() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, TEST_LDAP_NEW_PASSWORD);

      final var allowSignup = ldapConfigProperties.isAllowSignup();
      ldapConfigProperties.setAllowSignup(false);

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      mockMvc
          .perform(
              post("/auth/token")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(loginData)))
          .andExpect(status().isUnauthorized());

      ldapConfigProperties.setAllowSignup(allowSignup);
    }

    @Test
    void testSignupInvalidPassword() throws Exception {
      final var loginData = new LoginRequest(TEST_LDAP_NEW_USERNAME, "invalid!");

      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_LDAP_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      mockMvc
          .perform(
              post("/auth/token")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(loginData)))
          .andExpect(status().isUnauthorized());
    }

    private void doLogin(LoginRequest loginData) throws Exception {
      mockMvc
          .perform(
              post("/auth/token")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(loginData)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token", Matchers.not(Matchers.emptyString())));
    }
  }

  // To be precise, AuthenticationController is not involved with OAuth login, however,
  // to keep it all together, OAuth-related tests are in this place nevertheless
  @Nested
  @SpringBootTest
  @ExtendWith(SpringExtension.class)
  @AutoConfigureMockMvc
  @ActiveProfiles({"local_integration_test", "with_oauth"})
  class Oauth {

    private static final String OAUTH_TEST_PROVIDER = "testauth";
    private static final int WIREMOCK_PORT = 8484;

    private static final String TEST_OAUTH_NEW_USERNAME = "john.doe@frachtwerk.de";
    private static final String TEST_OAUTH_NEW_FIRST_NAME = "John";
    private static final String TEST_OAUTH_NEW_LAST_NAME = "Doe";
    private static final String TEST_OAUTH_NEW_ROLE_DEFAULT = "USER";
    private static final String TEST_OAUTH_NEW_ROLE_ATTR_SRC = "admin_role";
    private static final String TEST_OAUTH_NEW_ROLE_ATTR_DST = "ADMIN";

    private static final String TEST_OAUTH_EXISTING_USERNAME = "peter.pan@frachtwerk.de";
    private static final String TEST_OAUTH_EXISTING_FIRST_NAME = "Peter";
    private static final String TEST_OAUTH_EXISTING_LAST_NAME = "Pan";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestBaseUserRepository userRepository;
    @Autowired private RoleService roleService;
    @Autowired private TestingUtils testingUtils;
    @Autowired private ClientProperties clientProperties;
    @Autowired private JwtConfigProperties jwtConfigProperties;
    @Autowired private OAuthConfigProperties oAuthConfigProperties;

    private ClientRegistration clientRegistration;
    private ClientProvider clientProvider;

    private TestUser testUser;

    // automatically starts before and stops after each test
    @RegisterExtension WireMockExtension mockServer = new WireMockExtension(WIREMOCK_PORT);

    @BeforeEach
    public void setupSingle() {
      clientRegistration = clientProperties.getRegistration().get(OAUTH_TEST_PROVIDER);
      clientProvider = clientProperties.getProvider().get(OAUTH_TEST_PROVIDER);

      testingUtils.clearUsers();
      testUser =
          testingUtils.createUser(
              TestUserDto.builder()
                  .email(TEST_OAUTH_EXISTING_USERNAME)
                  .firstName(TEST_OAUTH_EXISTING_FIRST_NAME)
                  .lastName(TEST_OAUTH_EXISTING_LAST_NAME)
                  .password(RandomStringUtils.randomAlphanumeric(12))
                  .locale(Locale.GERMANY)
                  .source(OAUTH_TEST_PROVIDER)
                  .role(roleService.getDefaultRole().get().getName())
                  .build());
    }

    @AfterEach
    public void tearDownSingle() {
      testingUtils.clearUsers();
      userRepository
          .findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME)
          .ifPresent(user -> userRepository.deleteById(user.getId()));
      testUser = null;
    }

    @Test
    void testRedirectLoginScreen() throws Exception {
      mockMvc
          .perform(
              get("/oauth2/authorization/" + OAUTH_TEST_PROVIDER).accept(MediaType.TEXT_HTML_VALUE))
          .andExpect(status().isFound())
          .andExpect(
              header()
                  .string(
                      HttpHeaders.LOCATION,
                      Matchers.startsWith(clientProvider.getAuthorizationUri())))
          .andExpect(
              header()
                  .string(
                      HttpHeaders.LOCATION,
                      Matchers.containsString(
                          String.format("redirect_uri=%s", clientRegistration.getRedirectUri()))))
          .andExpect(
              header().string(HttpHeaders.LOCATION, Matchers.containsString("response_type=code")));
    }

    @Test
    void testLogin() throws Exception {
      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_OAUTH_EXISTING_USERNAME).isPresent(),
          Matchers.is(true));

      final var token =
          runOauth(
              Map.of(
                  "email", testUser.getUsername(),
                  "given_name", testUser.getFirstName(),
                  "family_name", testUser.getLastName()));

      testValidJwt(token, jwtConfigProperties, testUser);
    }

    @Test
    void testSignup() throws Exception {
      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      final var token =
          runOauth(
              Map.of(
                  "email", TEST_OAUTH_NEW_USERNAME,
                  "given_name", TEST_OAUTH_NEW_FIRST_NAME,
                  "family_name", TEST_OAUTH_NEW_LAST_NAME,
                  "user_role", "non_existing_role"));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getEmail(), Matchers.is(TEST_OAUTH_NEW_USERNAME));
      assertThat(newUser.get().getFirstName(), Matchers.is(TEST_OAUTH_NEW_FIRST_NAME));
      assertThat(newUser.get().getLastName(), Matchers.is(TEST_OAUTH_NEW_LAST_NAME));
      assertThat(newUser.get().getSource(), Matchers.is(OAUTH_TEST_PROVIDER));
      assertThat(newUser.get().getRole().getName(), Matchers.is(TEST_OAUTH_NEW_ROLE_DEFAULT));

      testValidJwt(token, jwtConfigProperties, newUser.get());
    }

    @Test
    void testSignupWithRoleMapping() throws Exception {
      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      final var token =
          runOauth(
              Map.of(
                  "email", TEST_OAUTH_NEW_USERNAME,
                  "given_name", TEST_OAUTH_NEW_FIRST_NAME,
                  "family_name", TEST_OAUTH_NEW_LAST_NAME,
                  "user_role", TEST_OAUTH_NEW_ROLE_ATTR_SRC));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getEmail(), Matchers.is(TEST_OAUTH_NEW_USERNAME));
      assertThat(newUser.get().getFirstName(), Matchers.is(TEST_OAUTH_NEW_FIRST_NAME));
      assertThat(newUser.get().getLastName(), Matchers.is(TEST_OAUTH_NEW_LAST_NAME));
      assertThat(newUser.get().getSource(), Matchers.is(OAUTH_TEST_PROVIDER));
      assertThat(newUser.get().getRole().getName(), Matchers.is(TEST_OAUTH_NEW_ROLE_ATTR_DST));

      testValidJwt(token, jwtConfigProperties, newUser.get());
    }

    @Test
    void testLoginUpdateRole() throws Exception {
      oAuthConfigProperties.setUpdateRole(true);

      runOauth(
          Map.of(
              "email", TEST_OAUTH_NEW_USERNAME,
              "given_name", TEST_OAUTH_NEW_FIRST_NAME,
              "family_name", TEST_OAUTH_NEW_LAST_NAME));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is("USER"));

      runOauth(
          Map.of(
              "email", TEST_OAUTH_NEW_USERNAME,
              "user_role", TEST_OAUTH_NEW_ROLE_ATTR_SRC));

      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(loggedInUser.get().getRole().getName(), Matchers.is(TEST_OAUTH_NEW_ROLE_ATTR_DST));

      assertEquals(newUser.get().getId(), loggedInUser.get().getId());
    }

    @Test
    void testLoginUpdateRoleFallbackToDefault() throws Exception {
      oAuthConfigProperties.setUpdateRole(true);

      runOauth(
          Map.of(
              "email", TEST_OAUTH_NEW_USERNAME,
              "given_name", TEST_OAUTH_NEW_FIRST_NAME,
              "family_name", TEST_OAUTH_NEW_LAST_NAME,
              "user_role", TEST_OAUTH_NEW_ROLE_ATTR_SRC));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is(TEST_OAUTH_NEW_ROLE_ATTR_DST));

      runOauth(Map.of("email", TEST_OAUTH_NEW_USERNAME));

      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(loggedInUser.get().getRole().getName(), Matchers.is("USER"));

      assertEquals(newUser.get().getId(), loggedInUser.get().getId());
    }

    @Test
    void testLoginNoUpdateRoleIfDisabled() throws Exception {
      oAuthConfigProperties.setUpdateRole(false);

      runOauth(
          Map.of(
              "email", TEST_OAUTH_NEW_USERNAME,
              "given_name", TEST_OAUTH_NEW_FIRST_NAME,
              "family_name", TEST_OAUTH_NEW_LAST_NAME));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getRole().getName(), Matchers.is("USER"));

      runOauth(
          Map.of(
              "email", TEST_OAUTH_NEW_USERNAME,
              "user_role", TEST_OAUTH_NEW_ROLE_ATTR_SRC));

      final var loggedInUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(loggedInUser.isPresent(), Matchers.is(true));
      assertThat(loggedInUser.get().getRole().getName(), Matchers.is("USER"));

      assertEquals(newUser.get().getId(), loggedInUser.get().getId());
    }

    @Test
    void testSignupCustomAttributes() throws Exception {
      assertThat(
          userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME).isPresent(),
          Matchers.is(false));

      final var tmpAttributes = clientRegistration.getAttributes();
      clientRegistration.setAttributes(
          ClientRegistrationAttributes.builder().firstname("vorname").build());

      final var token =
          runOauth(
              Map.of(
                  "email", TEST_OAUTH_NEW_USERNAME,
                  "vorname", TEST_OAUTH_NEW_FIRST_NAME,
                  "family_name", TEST_OAUTH_NEW_LAST_NAME));

      final var newUser = userRepository.findByEmailIgnoreCase(TEST_OAUTH_NEW_USERNAME);
      assertThat(newUser.isPresent(), Matchers.is(true));
      assertThat(newUser.get().getEmail(), Matchers.is(TEST_OAUTH_NEW_USERNAME));
      assertThat(newUser.get().getFirstName(), Matchers.is(TEST_OAUTH_NEW_FIRST_NAME));
      assertThat(newUser.get().getLastName(), Matchers.is(TEST_OAUTH_NEW_LAST_NAME));
      assertThat(newUser.get().getSource(), Matchers.is(OAUTH_TEST_PROVIDER));

      testValidJwt(token, jwtConfigProperties, newUser.get());

      clientRegistration.setAttributes(tmpAttributes);
    }

    private String runOauth(Map<String, String> userInfoResponseMap) throws Exception {
      // Step 1: Call redirect endpoint to register the auth request and get the state token
      // via org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
      final var redirectResult =
          mockMvc
              .perform(
                  get("/oauth2/authorization/" + OAUTH_TEST_PROVIDER)
                      .accept(MediaType.TEXT_HTML_VALUE))
              .andReturn();

      final var redirectTarget = redirectResult.getResponse().getHeader(HttpHeaders.LOCATION);
      final var redirectQueryParams =
          UriComponentsBuilder.fromHttpUrl(redirectTarget).build().getQueryParams();

      final var state =
          URLDecoder.decode(redirectQueryParams.getFirst("state"), Charset.defaultCharset());
      final var sessionState = RandomStringUtils.randomAlphanumeric(32);
      final var code = RandomStringUtils.randomAlphanumeric(32);
      final var accessToken = RandomStringUtils.randomAlphanumeric(32);
      final var httpSession = redirectResult.getRequest().getSession();

      mockServer.stubFor(
          WireMock.post(WireMock.urlPathEqualTo(new URL(clientProvider.getTokenUri()).getPath()))
              .willReturn(
                  WireMock.okJson(
                      objectMapper.writeValueAsString(
                          Map.of("access_token", accessToken, "token_type", "Bearer")))));

      mockServer.stubFor(
          WireMock.get(WireMock.urlPathEqualTo(new URL(clientProvider.getUserInfoUri()).getPath()))
              .willReturn(WireMock.okJson(objectMapper.writeValueAsString(userInfoResponseMap))));

      // Step 2: Call oauth login endpoint
      // via org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
      // via org.springframework.security.oauth2.client.web
      // HttpSessionOAuth2AuthorizationRequestRepository
      // Internally, the configured authorization server will be called twice:
      // 1. at the token url endpoint to exchange our fake code for an JWT access token
      // 2. at the user info url endpoint to request user data
      final var tokenResult =
          mockMvc
              .perform(
                  get("/login/oauth2/code/" + OAUTH_TEST_PROVIDER)
                      .session((MockHttpSession) httpSession) // reuse previous session
                      .accept(MediaType.TEXT_HTML_VALUE)
                      .queryParam("state", state)
                      .queryParam("session_state", sessionState)
                      .queryParam("code", code))
              .andExpect(status().isFound())
              .andExpect(header().string(HttpHeaders.LOCATION, Matchers.startsWith("/?token=")))
              .andReturn();

      return UriComponentsBuilder.fromUriString(
              tokenResult.getResponse().getHeader(HttpHeaders.LOCATION))
          .build()
          .getQueryParams()
          .getFirst("token");
    }
  }

  private static void testValidJwt(
      String token, JwtConfigProperties jwtConfigProperties, TestUser user) {
    final var secretKey = Encoders.BASE64.encode(jwtConfigProperties.getSecret().getBytes());
    final var jwt = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);

    assertThat(jwt.getBody().getIssuer(), Matchers.is(jwtConfigProperties.getIssuer()));
    assertThat(jwt.getBody().getSubject(), Matchers.is(user.getUsername()));
    assertThat(jwt.getBody().get("nonce", String.class), Matchers.not(Matchers.emptyString()));
    assertThat(jwt.getBody().get("given_name", String.class), Matchers.is(user.getFirstName()));
    assertThat(jwt.getBody().get("family_name", String.class), Matchers.is(user.getLastName()));
    assertThat(jwt.getBody().get("uid", Long.class), Matchers.is(user.getId()));

    final var issuedAt = jwt.getBody().getIssuedAt();
    final var expiresAt = jwt.getBody().getExpiration();

    assertThat(
        Duration.between(issuedAt.toInstant(), Instant.now()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.greaterThan(0), Matchers.lessThan(5 * 1000 * 1000) // no older than 5 seconds
            ));

    assertThat(
        Duration.between(Instant.now(), expiresAt.toInstant()).getNano() / 1000, // millis
        Matchers.allOf(
            Matchers.lessThan(jwtConfigProperties.getExpiration() * 1000 * 1000),
            Matchers.greaterThan(jwtConfigProperties.getExpiration() - 5 * 1000 * 1000),
            Matchers.greaterThan(0)));
  }
}
