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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpMethod;

/**
 * Removes an inherited global security requirement (e.g. set via {@code
 * OpenAPIDefinition#security}) from all operations that {@link WebSecurityConfig} makes accessible
 * without authentication. Operations with optional authentication get an additional empty
 * requirement, i.e. {@code security: [{}, {bearerAuth: []}]}. Operations declaring their own
 * security requirement are left untouched.
 *
 * <p>The access is determined by an {@link EndpointAccessResolver}, by default the rules of {@link
 * WebSecurityConfig} ({@link EssenciumEndpointAccess#of(HttpMethod, String)}).
 */
public class PublicEndpointsOpenApiCustomizer implements OpenApiCustomizer {

  private final EndpointAccessResolver accessResolver;

  public PublicEndpointsOpenApiCustomizer() {
    this(EssenciumEndpointAccess::of);
  }

  public PublicEndpointsOpenApiCustomizer(EndpointAccessResolver accessResolver) {
    this.accessResolver = accessResolver;
  }

  @Override
  public void customise(OpenAPI openApi) {
    List<SecurityRequirement> globalSecurity = openApi.getSecurity();
    if (globalSecurity == null || globalSecurity.isEmpty() || openApi.getPaths() == null) {
      return;
    }
    openApi
        .getPaths()
        .forEach((path, pathItem) ->
            pathItem
                .readOperationsMap()
                .forEach((method, operation) ->
                    customise(
                        operation,
                        accessResolver.resolve(HttpMethod.valueOf(method.name()), path),
                        globalSecurity)));
  }

  private void customise(
      Operation operation,
      EssenciumEndpointAccess.Access access,
      List<SecurityRequirement> globalSecurity) {
    if (Objects.nonNull(operation.getSecurity())) {
      return;
    }
    switch (access) {
      case PUBLIC -> operation.setSecurity(new ArrayList<>());
      case OPTIONAL -> {
        List<SecurityRequirement> security = new ArrayList<>();
        security.add(new SecurityRequirement());
        security.addAll(globalSecurity);
        operation.setSecurity(security);
      }
      case PROTECTED -> {
        // inherits the global security requirement
      }
    }
  }
}
