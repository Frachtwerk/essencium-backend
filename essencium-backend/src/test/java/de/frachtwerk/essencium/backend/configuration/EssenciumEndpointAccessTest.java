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

import de.frachtwerk.essencium.backend.configuration.EssenciumEndpointAccess.Access;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

class EssenciumEndpointAccessTest {

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
  void accessMatchesSecurityConfig(String method, String path, Access expected) {
    assertThat(EssenciumEndpointAccess.of(HttpMethod.valueOf(method), path)).isEqualTo(expected);

    // the request matchers used by WebSecurityConfig must agree with the documented access
    MockHttpServletRequest withoutAuth = new MockHttpServletRequest(method, path);
    MockHttpServletRequest withAuth = new MockHttpServletRequest(method, path);
    withAuth.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
    boolean publicWithoutAuth = EssenciumEndpointAccess.PUBLIC_URLS.matches(withoutAuth);
    boolean publicWithAuth = EssenciumEndpointAccess.PUBLIC_URLS.matches(withAuth);
    switch (expected) {
      case PUBLIC -> assertThat(publicWithoutAuth).isTrue();
      case OPTIONAL -> {
        assertThat(publicWithoutAuth).isTrue();
        assertThat(publicWithAuth).isFalse();
      }
      case PROTECTED -> {
        assertThat(publicWithoutAuth).isFalse();
        assertThat(EssenciumEndpointAccess.PROTECTED_URLS.matches(withoutAuth)).isTrue();
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"POST, /auth/renew", "POST, /auth/logout"})
  void controllerAuthEndpointsAreDocumentedAsProtected(String method, String path) {
    assertThat(EssenciumEndpointAccess.of(HttpMethod.valueOf(method), path))
        .isEqualTo(Access.PROTECTED);

    // the filter chain lets them pass, the controller checks the bearer token itself
    assertThat(
            EssenciumEndpointAccess.PUBLIC_URLS.matches(new MockHttpServletRequest(method, path)))
        .isTrue();
  }

  @Test
  void otherAuthEndpointsStayPublic() {
    assertThat(EssenciumEndpointAccess.of(HttpMethod.GET, "/auth/renew")).isEqualTo(Access.PUBLIC);
    assertThat(EssenciumEndpointAccess.of(HttpMethod.POST, "/auth/token")).isEqualTo(Access.PUBLIC);
  }
}
