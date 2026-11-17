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
import de.frachtwerk.essencium.backend.configuration.EndpointAccessCustomizer;
import de.frachtwerk.essencium.backend.configuration.EssenciumEndpointAccess.Rule;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
  EndpointAccessCustomizerTest.AccessConfig.class,
  EndpointAccessCustomizerTest.V2Controller.class
})
class EndpointAccessCustomizerTest {

  @TestConfiguration
  static class AccessConfig {
    @Bean
    EndpointAccessCustomizer v2Access() {
      return rules -> {
        rules.protect("/v2/**").permit(HttpMethod.GET, "/v2/status");
        rules.publicRules().remove(Rule.of(HttpMethod.GET, "/v1/translations/**"));
      };
    }
  }

  @RestController
  static class V2Controller {
    @GetMapping("/v2/orders")
    String orders() {
      return "orders";
    }

    @GetMapping("/v2/status")
    String status() {
      return "ok";
    }
  }

  @LocalServerPort private int port;

  @Test
  void customizedRulesApplyToSecurity() throws Exception {
    assertThat(status("/v2/orders", null)).isEqualTo(401);
    assertThat(status("/v2/orders", login())).isEqualTo(200);
    assertThat(status("/v2/status", null)).isEqualTo(200);
    assertThat(status("/v1/translations/de", null)).isEqualTo(401);
    assertThat(status("/v1/translations/de", login())).isEqualTo(200);
  }

  @Test
  void customizedRulesApplyToDocumentation() throws Exception {
    JsonNode paths =
        new ObjectMapper()
            .readTree(client().get().uri("/v3/api-docs").retrieve().body(String.class))
            .get("paths");

    assertThat(paths.path("/v2/orders").path("get").has("security")).isFalse();
    assertThat(paths.path("/v2/status").path("get").get("security").isEmpty()).isTrue();
    assertThat(paths.path("/v1/translations/{locale}").path("get").has("security")).isFalse();
    assertThat(paths.path("/auth/token").path("post").get("security").isEmpty()).isTrue();
  }

  private int status(String uri, String token) {
    return client()
        .get()
        .uri(uri)
        .headers(headers -> {
          if (token != null) {
            headers.setBearerAuth(token);
          }
        })
        .exchange((request, response) -> response.getStatusCode())
        .value();
  }

  private String login() throws Exception {
    String response =
        client()
            .post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.USER_AGENT, "test")
            .body(Map.of("username", "devnull@frachtwerk.de", "password", "adminAdminAdmin"))
            .retrieve()
            .body(String.class);
    return new ObjectMapper().readTree(response).get("token").asText();
  }

  private RestClient client() {
    return RestClient.create("http://localhost:" + port);
  }
}
