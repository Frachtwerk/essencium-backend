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

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicEndpointsOpenApiCustomizerTest {

  private static final SecurityRequirement BEARER = new SecurityRequirement().addList("bearerAuth");

  private final PublicEndpointsOpenApiCustomizer customizer =
      new PublicEndpointsOpenApiCustomizer();

  private OpenAPI openApi;

  @BeforeEach
  void setUp() {
    openApi =
        new OpenAPI()
            .security(new ArrayList<>(List.of(BEARER)))
            .paths(
                new Paths()
                    .addPathItem("/auth/token", new PathItem().post(new Operation()))
                    .addPathItem(
                        "/v1/translations/{locale}",
                        new PathItem().get(new Operation()).put(new Operation()))
                    .addPathItem("/v1/contact", new PathItem().post(new Operation()))
                    .addPathItem("/v1/users", new PathItem().get(new Operation())));
  }

  @Test
  void publicOperationsHaveNoSecurityRequirement() {
    customizer.customise(openApi);

    assertThat(operation("/auth/token").getPost().getSecurity()).isEmpty();
    assertThat(operation("/v1/translations/{locale}").getGet().getSecurity()).isEmpty();
  }

  @Test
  void protectedOperationsInheritGlobalSecurityRequirement() {
    customizer.customise(openApi);

    assertThat(operation("/v1/translations/{locale}").getPut().getSecurity()).isNull();
    assertThat(operation("/v1/users").getGet().getSecurity()).isNull();
  }

  @Test
  void optionalAuthOperationsAllowAnonymousAndGlobalSecurity() {
    customizer.customise(openApi);

    assertThat(operation("/v1/contact").getPost().getSecurity())
        .containsExactly(new SecurityRequirement(), BEARER);
  }

  @Test
  void explicitOperationSecurityIsKept() {
    List<SecurityRequirement> explicit = List.of(new SecurityRequirement().addList("apiKey"));
    operation("/auth/token").getPost().setSecurity(explicit);

    customizer.customise(openApi);

    assertThat(operation("/auth/token").getPost().getSecurity()).isEqualTo(explicit);
  }

  @Test
  void nothingChangesWithoutGlobalSecurity() {
    openApi.setSecurity(null);

    customizer.customise(openApi);

    assertThat(operation("/auth/token").getPost().getSecurity()).isNull();
    assertThat(operation("/v1/contact").getPost().getSecurity()).isNull();
  }

  @Test
  void customResolverOverridesDefaultRules() {
    new PublicEndpointsOpenApiCustomizer((method, path) ->
        path.startsWith("/auth/")
            ? EssenciumEndpointAccess.Access.PROTECTED
            : EssenciumEndpointAccess.of(method, path))
        .customise(openApi);

    assertThat(operation("/auth/token").getPost().getSecurity()).isNull();
    assertThat(operation("/v1/translations/{locale}").getGet().getSecurity()).isEmpty();
  }

  @Test
  void serializesOverridesExplicitly() throws Exception {
    customizer.customise(openApi);

    String json = Json.mapper().writeValueAsString(openApi);

    assertThat(json)
        .contains("\"/auth/token\":{\"post\":{\"security\":[]}}")
        .contains("\"/v1/contact\":{\"post\":{\"security\":[{},{\"bearerAuth\":[]}]}}");
  }

  private PathItem operation(String path) {
    return openApi.getPaths().get(path);
  }
}
