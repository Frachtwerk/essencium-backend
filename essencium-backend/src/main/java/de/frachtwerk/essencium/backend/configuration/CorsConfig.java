/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.configuration.properties.AppCorsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CorsConfig {
  private final AppCorsProperties appCorsProperties;

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // Set allowed origin patterns from configuration (supports wildcards with credentials)
    // allowedOriginPatterns takes precedence over allowedOrigins if both are set
    if (appCorsProperties.getAllowedOriginPatterns() != null
        && !appCorsProperties.getAllowedOriginPatterns().isEmpty()) {
      config.setAllowedOriginPatterns(appCorsProperties.getAllowedOriginPatterns());
      log.info(
          "CORS: Allowed origin patterns configured: {}",
          appCorsProperties.getAllowedOriginPatterns());

      if (appCorsProperties.getAllowedOriginPatterns().contains("*")) {
        log.warn(
            "CORS: Using wildcard origin pattern '*' - ALL origins are allowed! "
                + "This should only be used in development environments.");
      }
    }
    // Fallback to allowed origins (no wildcard support with credentials)
    else if (appCorsProperties.getAllowedOrigins() != null
        && !appCorsProperties.getAllowedOrigins().isEmpty()) {
      config.setAllowedOrigins(appCorsProperties.getAllowedOrigins());
      log.info("CORS: Allowed origins configured: {}", appCorsProperties.getAllowedOrigins());
    } else {
      log.warn("CORS: No allowed origins or origin patterns configured. CORS requests may fail.");
    }

    // Enable credentials (required for cookies and Authorization headers)
    config.setAllowCredentials(appCorsProperties.isAllowCredentials());
    log.info("CORS: Allow credentials: {}", appCorsProperties.isAllowCredentials());

    // Set allowed HTTP methods
    if (appCorsProperties.getAllowedMethods() != null
        && !appCorsProperties.getAllowedMethods().isEmpty()) {
      config.setAllowedMethods(appCorsProperties.getAllowedMethods());
    }

    // Set allowed headers
    if (appCorsProperties.getAllowedHeaders() != null
        && !appCorsProperties.getAllowedHeaders().isEmpty()) {
      config.setAllowedHeaders(appCorsProperties.getAllowedHeaders());
    }

    // Expose headers (important for Authorization header with JWT tokens)
    if (appCorsProperties.getExposedHeaders() != null
        && !appCorsProperties.getExposedHeaders().isEmpty()) {
      config.setExposedHeaders(appCorsProperties.getExposedHeaders());
    }

    // Set preflight cache max age
    if (appCorsProperties.getMaxAge() != null) {
      config.setMaxAge(appCorsProperties.getMaxAge());
    }

    // Apply CORS configuration to all paths
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }
}
