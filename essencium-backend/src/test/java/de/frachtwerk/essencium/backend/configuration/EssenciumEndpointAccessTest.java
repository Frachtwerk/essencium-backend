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

package de.frachtwerk.essencium.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.frachtwerk.essencium.backend.configuration.EssenciumEndpointAccess.Access;
import de.frachtwerk.essencium.backend.configuration.EssenciumEndpointAccess.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

class EssenciumEndpointAccessTest {

  private static final EssenciumEndpointAccess DEFAULTS = EssenciumEndpointAccess.defaults();

  @ParameterizedTest
  @CsvSource({
    "POST, /auth/token, PUBLIC",
    "GET, /auth/oauth-registrations, PUBLIC",
    "POST, /sentry/feedback, PUBLIC",
    "GET, /v3/api-docs, PUBLIC",
    "GET, /v1/translations, PUBLIC",
    "GET, /v1/translations/{locale}, PUBLIC",
    "PUT, /v1/translations/{locale}, PROTECTED",
    "DELETE, /v1/translations/{locale}/{key}, PROTECTED",
    "POST, /v1/reset-credentials, PUBLIC",
    "POST, /v1/set-password, PUBLIC",
    "POST, /v1/contact, OPTIONAL",
    "GET, /v1, PROTECTED",
    "GET, /v1/users, PROTECTED",
    "GET, /v1/users/{id}, PROTECTED",
    "GET, /v1/translationsfoo, PROTECTED",
  })
  void defaultAccessMatchesRequestMatchers(String method, String path, Access expected) {
    assertAccess(DEFAULTS, method, path, expected);
  }

  @ParameterizedTest
  @CsvSource({"POST, /auth/renew", "POST, /auth/logout"})
  void controllerAuthEndpointsAreDocumentedAsProtected(String method, String path) {
    assertThat(DEFAULTS.of(HttpMethod.valueOf(method), path)).isEqualTo(Access.PROTECTED);

    // the filter chain lets them pass, the controller checks the bearer token itself
    assertThat(DEFAULTS.publicUrls().matches(new MockHttpServletRequest(method, path))).isTrue();
  }

  @Test
  void otherAuthEndpointsStayPublic() {
    assertThat(DEFAULTS.of(HttpMethod.GET, "/auth/renew")).isEqualTo(Access.PUBLIC);
    assertThat(DEFAULTS.of(HttpMethod.POST, "/auth/token")).isEqualTo(Access.PUBLIC);
  }

  @Test
  void additionalRulesExtendDefaults() {
    EssenciumEndpointAccess access =
        EssenciumEndpointAccess.builder()
            .withDefaults()
            .protect("/v2/**")
            .permit(HttpMethod.GET, "/v2/status")
            .optionalAuth("/v2/feedback/**")
            .build();

    assertAccess(access, "GET", "/v2/orders", Access.PROTECTED);
    assertAccess(access, "GET", "/v2/status", Access.PUBLIC);
    assertAccess(access, "POST", "/v2/status", Access.PROTECTED);
    assertAccess(access, "POST", "/v2/feedback", Access.OPTIONAL);
    assertAccess(access, "GET", "/v1/users", Access.PROTECTED);
    assertAccess(access, "POST", "/auth/token", Access.PUBLIC);
  }

  @Test
  void defaultRulesCanBeOverridden() {
    EssenciumEndpointAccess.Builder builder = EssenciumEndpointAccess.builder().withDefaults();
    builder.publicRules().remove(Rule.of(HttpMethod.GET, "/v1/translations/**"));
    builder.optionalAuthRules().clear();
    EssenciumEndpointAccess access = builder.build();

    assertAccess(access, "GET", "/v1/translations", Access.PROTECTED);
    assertAccess(access, "POST", "/v1/contact", Access.PROTECTED);
    assertThat(access.publicRules()).doesNotContain(Rule.of(HttpMethod.GET, "/v1/translations/**"));
  }

  @Test
  void templatesWithConstrainedVariablesAreDocumentedAsProtected() {
    EssenciumEndpointAccess access =
        EssenciumEndpointAccess.builder().protect("/v2/orders/{id:[0-9]+}").build();

    assertThat(access.of(HttpMethod.GET, "/v2/orders/{id}")).isEqualTo(Access.PROTECTED);
    assertAccess(access, "GET", "/v2/orders/123", Access.PROTECTED);
    assertAccess(access, "GET", "/v2/orders/abc", Access.PUBLIC);
  }

  @Test
  void templatesPartlyCoveredByProtectedLiteralAreDocumentedAsProtected() {
    EssenciumEndpointAccess access =
        EssenciumEndpointAccess.builder()
            .protect("/v2/orders/export")
            .protect("/v2/order-*")
            .build();

    assertThat(access.of(HttpMethod.GET, "/v2/orders/{id}")).isEqualTo(Access.PROTECTED);
    assertThat(access.of(HttpMethod.GET, "/v2/order-{id}")).isEqualTo(Access.PROTECTED);
    assertThat(access.of(HttpMethod.GET, "/v2/orders/{id}/items")).isEqualTo(Access.PUBLIC);
  }

  @Test
  void templatesArePublicOrOptionalOnlyIfAllValuesAre() {
    EssenciumEndpointAccess access =
        EssenciumEndpointAccess.builder()
            .protect("/v2/**")
            .permit("/v2/orders/{id:[0-9]+}")
            .permit("/v2/status/{*rest}")
            .optionalAuth("/v2/feedback/*")
            .optionalAuth("/v2/comments/{id:[0-9]+}")
            .build();

    assertThat(access.of(HttpMethod.GET, "/v2/orders/{id}")).isEqualTo(Access.PROTECTED);
    assertAccess(access, "GET", "/v2/orders/123", Access.PUBLIC);
    assertThat(access.of(HttpMethod.GET, "/v2/status/{name}/{detail}")).isEqualTo(Access.PUBLIC);
    assertThat(access.of(HttpMethod.POST, "/v2/feedback/{id}")).isEqualTo(Access.OPTIONAL);
    assertThat(access.of(HttpMethod.POST, "/v2/comments/{id}")).isEqualTo(Access.PROTECTED);
    assertThat(access.of(HttpMethod.GET, "/v3/orders/{id}")).isEqualTo(Access.PUBLIC);
  }

  @Test
  void emptyRulesMakeEverythingPublic() {
    EssenciumEndpointAccess access = EssenciumEndpointAccess.builder().build();

    assertAccess(access, "GET", "/v1/users", Access.PUBLIC);
  }

  @Test
  void builtRulesAreImmutable() {
    assertThatThrownBy(() -> DEFAULTS.protectedRules().add(Rule.of("/v2/**")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /** the request matchers used by WebSecurityConfig must agree with the documented access */
  private static void assertAccess(
      EssenciumEndpointAccess access, String method, String path, Access expected) {
    assertThat(access.of(HttpMethod.valueOf(method), path))
        .as("%s %s", method, path)
        .isEqualTo(expected);

    MockHttpServletRequest withoutAuth = new MockHttpServletRequest(method, path);
    MockHttpServletRequest withAuth = new MockHttpServletRequest(method, path);
    withAuth.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
    boolean publicWithoutAuth = access.publicUrls().matches(withoutAuth);
    boolean publicWithAuth = access.publicUrls().matches(withAuth);
    switch (expected) {
      case PUBLIC -> assertThat(publicWithoutAuth).as("%s %s", method, path).isTrue();
      case OPTIONAL -> {
        assertThat(publicWithoutAuth).as("%s %s", method, path).isTrue();
        assertThat(publicWithAuth).as("%s %s", method, path).isFalse();
      }
      case PROTECTED -> {
        assertThat(publicWithoutAuth).as("%s %s", method, path).isFalse();
        assertThat(access.protectedUrls().matches(withoutAuth)).as("%s %s", method, path).isTrue();
      }
    }
  }
}
