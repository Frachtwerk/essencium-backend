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
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiGroupSecurityTest {

  @TestConfiguration
  static class GroupConfig {
    @Bean
    GroupedOpenApi essenciumGroup() {
      return GroupedOpenApi.builder().group("essencium").pathsToMatch("/**").build();
    }
  }

  @LocalServerPort private int port;

  @Test
  void groupSpecHasPublicOperationsWithoutSecurityRequirement() throws Exception {
    String json =
        RestClient.create("http://localhost:" + port)
            .get()
            .uri("/v3/api-docs/essencium")
            .retrieve()
            .body(String.class);
    JsonNode paths = new ObjectMapper().readTree(json).get("paths");

    JsonNode token = paths.path("/auth/token").path("post");
    assertThat(token.isMissingNode()).isFalse();
    assertThat(token.get("security")).isNotNull();
    assertThat(token.get("security").isEmpty()).isTrue();
    assertThat(paths.path("/v1/users").path("get").has("security")).isFalse();
  }
}
