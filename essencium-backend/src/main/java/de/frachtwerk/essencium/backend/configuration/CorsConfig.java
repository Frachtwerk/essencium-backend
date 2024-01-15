/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import java.util.Arrays;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for CORS.
 *
 * @deprecated since 2.5.2, forRemoval in next major version
 * @implNote
 *     <p>Use {@link
 *     org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties} for
 *     configuration instead.
 *     <p>You may use the following environment variables in your application:
 *     <ul>
 *       <li>management.endpoints.web.cors.allowed-origins
 *       <li>management.endpoints.web.cors.allowed-origin-patterns
 *       <li>management.endpoints.web.cors.allowed-methods
 *       <li>management.endpoints.web.cors.allowed-headers
 *       <li>management.endpoints.web.cors.exposed-headers
 *       <li>management.endpoints.web.cors.allow-credentials
 *       <li>management.endpoints.web.cors.max-age
 *     </ul>
 */
@Configuration
@ConditionalOnProperty(value = "app.cors.allow", havingValue = "true")
@Deprecated(since = "2.5.2", forRemoval = true)
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedMethods(
            Arrays.stream(HttpMethod.values())
                .map(HttpMethod::name)
                .toList()
                .toArray(new String[] {}));
  }
}
