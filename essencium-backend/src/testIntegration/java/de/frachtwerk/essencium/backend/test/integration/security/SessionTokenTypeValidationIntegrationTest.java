/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.test.integration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppTokenProperties;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.app.model.entity.TestUser;
import de.frachtwerk.essencium.backend.test.integration.util.AbstractEssenciumIntegrationTest;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SessionTokenTypeValidationIntegrationTest extends AbstractEssenciumIntegrationTest {

  private static final String API_PSK_HEADER = "X-API-Token-PSK";

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final TestingUtils testingUtils;
  private final AppTokenProperties appTokenProperties;

  private LoginTokens loginTokens;
  private String apiToken;

  private Set<String> originalAllowedIpAddresses;
  private Set<String> originalTrustedProxies;
  private String originalPresharedSecretHeaderName;
  private Set<String> originalPresharedSecrets;

  @Autowired
  SessionTokenTypeValidationIntegrationTest(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      TestingUtils testingUtils,
      AppTokenProperties appTokenProperties) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.testingUtils = testingUtils;
    this.appTokenProperties = appTokenProperties;
  }

  @BeforeEach
  void setUp() throws Exception {
    testingUtils.clearUsers();
    testingUtils.clearRoles();
    testingUtils.clearRights();

    originalAllowedIpAddresses = new HashSet<>(appTokenProperties.getAllowedIpAddresses());
    originalTrustedProxies = new HashSet<>(appTokenProperties.getTrustedProxies());
    originalPresharedSecretHeaderName = appTokenProperties.getPresharedSecretHeaderName();
    originalPresharedSecrets = new HashSet<>(appTokenProperties.getPresharedSecrets());

    resetTokenPolicy();

    TestUser testUser = testingUtils.createRandomUser();
    loginTokens = login(testUser.getEmail(), TestingUtils.DEFAULT_PASSWORD);
    apiToken = testingUtils.createApiTokenForUser(testUser).getToken();
  }

  @AfterEach
  void tearDown() {
    appTokenProperties.setAllowedIpAddresses(originalAllowedIpAddresses);
    appTokenProperties.setTrustedProxies(originalTrustedProxies);
    appTokenProperties.setPresharedSecretHeaderName(originalPresharedSecretHeaderName);
    appTokenProperties.setPresharedSecrets(originalPresharedSecrets);

    testingUtils.clearUsers();
    testingUtils.clearRoles();
    testingUtils.clearRights();
  }

  @Nested
  @DisplayName("SessionTokenType.REFRESH Tests")
  class RefreshTokenTests {
    private static final String RENEW_ENDPOINT = "/auth/renew";

    @Test
    @DisplayName("REFRESH token must be rejected on /v1 endpoints")
    void refreshTokenOnProtectedEndpoint_isUnauthorized() throws Exception {
      assertStatusAndMessage(
          protectedCall(loginTokens.refreshToken(), "9.9.9.9", null, null),
          401,
          "Refresh token is only allowed for /auth/renew via refreshToken cookie");
    }

    @Test
    @DisplayName("/auth/renew requires refresh token in cookie")
    void renewWithoutCookie_isBadRequest() throws Exception {
      assertStatusAndMessage(
          mockMvc.perform(
              post(RENEW_ENDPOINT)
                  .header(HttpHeaders.USER_AGENT, "JUnit")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginTokens.accessToken())),
          400,
          "Required cookie 'refreshToken'");
    }

    @Test
    @DisplayName("/auth/renew without access token header is unauthorized")
    void renewWithoutAccessToken_isUnauthorized() throws Exception {
      assertStatusAndMessage(
          mockMvc.perform(
              post(RENEW_ENDPOINT)
                  .header(HttpHeaders.USER_AGENT, "JUnit")
                  .cookie(new Cookie("refreshToken", loginTokens.refreshToken()))),
          401,
          "No access token provided");
    }

    @Test
    @DisplayName("/auth/renew with mismatching access token is unauthorized")
    void renewWithMismatchingAccessToken_isUnauthorized() throws Exception {
      TestUser secondUser = testingUtils.createRandomUser();
      LoginTokens secondUserTokens = login(secondUser.getEmail(), TestingUtils.DEFAULT_PASSWORD);

      assertStatusAndMessage(
          mockMvc.perform(
              post(RENEW_ENDPOINT)
                  .header(HttpHeaders.USER_AGENT, "JUnit")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondUserTokens.accessToken())
                  .cookie(new Cookie("refreshToken", loginTokens.refreshToken()))),
          401,
          "Refresh token and access token do not belong together");
    }

    @Test
    @DisplayName("/auth/renew with matching access+refresh token succeeds")
    void renewWithMatchingTokens_isOk() throws Exception {
      mockMvc
          .perform(
              post(RENEW_ENDPOINT)
                  .header(HttpHeaders.USER_AGENT, "JUnit")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginTokens.accessToken())
                  .cookie(new Cookie("refreshToken", loginTokens.refreshToken())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("SessionTokenType.ACCESS Tests")
  class AccessTokenTests {

    @Test
    @DisplayName("ACCESS token must not be constrained by API whitelist/PSK settings")
    void accessTokenSkipsApiWhitelistAndPskChecks() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      mockMvc
          .perform(
              get("/v1/users/me/roles")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginTokens.accessToken())
                  .with(
                      request -> {
                        request.setRemoteAddr("9.9.9.9");
                        return request;
                      }))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("SessionTokenType.API Tests")
  class ApiTokenTests {

    @Test
    void apiToken_whitelistOnly_allowedIp_isOk() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "1.2.3.4", null, null).andExpect(status().isOk());
    }

    @Test
    void apiToken_whitelistOnly_deniedIp_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecrets(Set.of());

      assertStatusAndMessage(
          protectedCall(apiToken, "9.9.9.9", null, null),
          403,
          "IP address not allowed to use API tokens");
    }

    @Test
    void apiToken_whitelistAndPsk_validIpAndPsk_isOk() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      protectedCall(apiToken, "1.2.3.4", null, "secret-1").andExpect(status().isOk());
    }

    @Test
    void apiToken_whitelistAndPsk_missingPsk_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      assertStatusAndMessage(
          protectedCall(apiToken, "1.2.3.4", null, null), 403, "Invalid preshared secret");
    }

    @Test
    void apiToken_whitelistAndPsk_wrongPsk_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      assertStatusAndMessage(
          protectedCall(apiToken, "1.2.3.4", null, "wrong-secret"),
          403,
          "Invalid preshared secret");
    }

    @Test
    void apiToken_whitelistAndPsk_deniedIp_isForbiddenWithIpMessage() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      assertStatusAndMessage(
          protectedCall(apiToken, "9.9.9.9", null, "secret-1"),
          403,
          "IP address not allowed to use API tokens");
    }

    @Test
    void apiToken_pskOnly_validPsk_isOk() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of());
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      protectedCall(apiToken, "9.9.9.9", null, "secret-1").andExpect(status().isOk());
    }

    @Test
    void apiToken_pskOnly_missingOrWrongPsk_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of());
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
      appTokenProperties.setPresharedSecrets(Set.of("secret-1"));

      assertStatusAndMessage(
          protectedCall(apiToken, "9.9.9.9", null, null), 403, "Invalid preshared secret");
      assertStatusAndMessage(
          protectedCall(apiToken, "9.9.9.9", null, "wrong-secret"),
          403,
          "Invalid preshared secret");
    }

    @Test
    void apiToken_trustedProxiesEmpty_spoofedXffIgnored_remoteAddrUsed() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecrets(Set.of());

      assertStatusAndMessage(
          protectedCall(apiToken, "9.9.9.9", "1.2.3.4", null),
          403,
          "IP address not allowed to use API tokens");
    }

    @Test
    void apiToken_trustedProxy_singleHop_usesClientFromXff() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.1"));
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "10.0.0.1", "1.2.3.4", null).andExpect(status().isOk());
    }

    @Test
    void apiToken_trustedProxy_cidr_usesClientFromXff() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.0/8"));
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "10.1.2.3", "1.2.3.4", null).andExpect(status().isOk());
    }

    @Test
    void apiToken_trustedProxy_multiHop_walksRightToLeft() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.1", "10.0.0.2"));
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "10.0.0.1", "1.2.3.4, 10.0.0.2", null).andExpect(status().isOk());
    }

    @Test
    void apiToken_trustedProxy_allHopsTrusted_fallsBackToLeftmost() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("10.0.0.2"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.1", "10.0.0.2"));
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "10.0.0.1", "10.0.0.2", null).andExpect(status().isOk());
    }

    @Test
    void apiToken_trustedProxy_noXff_directClientUsed() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.1"));
      appTokenProperties.setPresharedSecrets(Set.of());

      protectedCall(apiToken, "1.2.3.4", null, null).andExpect(status().isOk());
    }

    @Test
    void apiToken_trustedProxy_invalidXffEntry_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of("10.0.0.1", "10.0.0.2"));
      appTokenProperties.setPresharedSecrets(Set.of());

      assertStatusAndMessage(
          protectedCall(apiToken, "10.0.0.1", "unknown, 10.0.0.2", null),
          403,
          "IP address not allowed to use API tokens");
    }

    @Test
    void apiToken_blankRemoteAddr_isForbidden() throws Exception {
      appTokenProperties.setAllowedIpAddresses(Set.of("1.2.3.4"));
      appTokenProperties.setTrustedProxies(Set.of());
      appTokenProperties.setPresharedSecrets(Set.of());

      assertStatusAndMessage(
          protectedCall(apiToken, "", null, null), 403, "Unable to determine remote IP address");
    }
  }

  private void resetTokenPolicy() {
    appTokenProperties.setAllowedIpAddresses(Set.of());
    appTokenProperties.setTrustedProxies(Set.of());
    appTokenProperties.setPresharedSecretHeaderName(API_PSK_HEADER);
    appTokenProperties.setPresharedSecrets(Set.of());
  }

  private ResultActions protectedCall(
      String token, String remoteAddr, String xForwardedFor, String psk) throws Exception {
    var requestBuilder =
        get("/v1/users/me/roles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .with(
                request -> {
                  request.setRemoteAddr(remoteAddr);
                  return request;
                });

    if (xForwardedFor != null) {
      requestBuilder.header("X-Forwarded-For", xForwardedFor);
    }

    if (psk != null) {
      requestBuilder.header(appTokenProperties.getPresharedSecretHeaderName(), psk);
    }

    return mockMvc.perform(requestBuilder);
  }

  private LoginTokens login(String username, String password) throws Exception {
    LoginRequest loginRequest = new LoginRequest(username, password);
    String body = objectMapper.writeValueAsString(loginRequest);

    var result =
        mockMvc
            .perform(
                post("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.USER_AGENT, "JUnit")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    String accessToken = json.get("token").asString();
    Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
    String refreshToken =
        Objects.requireNonNull(refreshCookie, "refreshToken cookie missing").getValue();

    return new LoginTokens(accessToken, refreshToken);
  }

  private void assertStatusAndMessage(
      ResultActions action, int expectedStatus, String expectedMessage) throws Exception {
    action
        .andExpect(status().is(expectedStatus))
        .andExpect(
            result -> {
              String body = Objects.toString(result.getResponse().getContentAsString(), "");
              String errorMessage = Objects.toString(result.getResponse().getErrorMessage(), "");
              assertThat(body + "\n" + errorMessage).contains(expectedMessage);
            });
  }

  private record LoginTokens(String accessToken, String refreshToken) {}
}
