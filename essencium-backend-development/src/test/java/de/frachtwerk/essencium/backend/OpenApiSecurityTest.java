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

package de.frachtwerk.essencium.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiSecurityTest {

  @LocalServerPort private int port;

  private JsonNode paths;

  @BeforeAll
  void loadApiDocs() throws Exception {
    String json =
        RestClient.create("http://localhost:" + port)
            .get()
            .uri("/v3/api-docs")
            .retrieve()
            .body(String.class);
    JsonNode apiDocs = new ObjectMapper().readTree(json);
    assertThat(apiDocs.at("/security/0/bearerAuth").isArray()).isTrue();
    paths = apiDocs.get("paths");
  }

  @Test
  void publicOperationsHaveNoSecurityRequirement() {
    assertNoSecurity("/auth/token", "post");
    assertNoSecurity("/auth/oauth-registrations", "get");
    assertNoSecurity("/v1/reset-credentials", "post");
    assertNoSecurity("/v1/set-password", "post");
    assertNoSecurity("/v1/translations", "get");
    assertNoSecurity("/v1/translations/{locale}", "get");
    assertNoSecurity("/sentry/feedback", "post");
  }

  @Test
  void protectedOperationsInheritGlobalSecurityRequirement() {
    assertThat(operation("/v1/translations/{locale}", "put").has("security")).isFalse();
    assertThat(operation("/v1/users", "get").has("security")).isFalse();
  }

  @Test
  void operationsWithTokenCheckInControllerInheritGlobalSecurityRequirement() {
    assertThat(operation("/auth/renew", "post").has("security")).isFalse();
    assertThat(operation("/auth/logout", "post").has("security")).isFalse();
  }

  @Test
  void contactAllowsAnonymousAndBearerAuth() {
    JsonNode security = operation("/v1/contact", "post").get("security");
    assertThat(security).hasSize(2);
    assertThat(security.get(0).isEmpty()).isTrue();
    assertThat(security.get(1).get("bearerAuth").isArray()).isTrue();
  }

  private void assertNoSecurity(String path, String method) {
    JsonNode security = operation(path, method).get("security");
    assertThat(security).as("%s %s", method, path).isNotNull();
    assertThat(security.isEmpty()).as("%s %s", method, path).isTrue();
  }

  private JsonNode operation(String path, String method) {
    JsonNode operation = paths.path(path).path(method);
    assertThat(operation.isMissingNode()).as("%s %s missing", method, path).isFalse();
    return operation;
  }
}
