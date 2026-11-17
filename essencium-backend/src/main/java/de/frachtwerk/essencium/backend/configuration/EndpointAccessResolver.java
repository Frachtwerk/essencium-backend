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

import org.springframework.http.HttpMethod;

/**
 * Determines the documented access of an API operation. Applications that change the access rules
 * of {@link WebSecurityConfig} can register a bean of this type to adjust the OpenAPI security
 * requirements accordingly, see {@link PublicEndpointsOpenApiCustomizer}.
 */
@FunctionalInterface
public interface EndpointAccessResolver {

  /**
   * @param method HTTP method of the operation
   * @param path path of the operation as documented, e.g. {@code /v1/users/{id}}
   */
  EssenciumEndpointAccess.Access resolve(HttpMethod method, String path);
}
