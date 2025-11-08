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

package de.frachtwerk.essencium.backend.configuration.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {
  /**
   * List of allowed origins for CORS requests. Example: ["http://localhost:3000",
   * "https://app.example.com"] Note: When allowCredentials is true, wildcards (*) are not allowed
   * in allowedOrigins. Use allowedOriginPatterns instead for wildcard support with credentials.
   */
  private List<String> allowedOrigins =
      new ArrayList<>(
          List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8098"));

  /**
   * List of allowed origin patterns for CORS requests (supports wildcards). This allows wildcards
   * even when allowCredentials is true. Examples: - "*" - allows ALL origins (dynamically mirrors
   * the requesting origin) - "https://*.example.com" - allows all subdomains of example.com -
   * "http://localhost:*" - allows all ports on localhost
   *
   * <p>Note: If both allowedOrigins and allowedOriginPatterns are set, allowedOriginPatterns takes
   * precedence.
   *
   * <p>SECURITY WARNING: Using "*" allows ANY origin. Use with caution! Consider using specific
   * patterns in production.
   */
  private List<String> allowedOriginPatterns = new ArrayList<>();

  /**
   * Whether to allow credentials (cookies, authorization headers) in CORS requests. Should be true
   * when using JWT tokens in cookies or Authorization headers. Default: true (required for
   * authentication with cookies and Bearer tokens)
   */
  private boolean allowCredentials = true;

  /** List of allowed HTTP methods for CORS requests. Default: all standard methods */
  private List<String> allowedMethods =
      new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

  /** List of allowed headers in CORS requests. Default: all headers (*) */
  private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

  /**
   * List of headers that should be exposed to the client. The Authorization header is required for
   * JWT token responses.
   */
  private List<String> exposedHeaders = new ArrayList<>(List.of("Authorization"));

  /** Maximum age (in seconds) for CORS preflight request caching. Default: 3600 seconds (1 hour) */
  private Long maxAge = 3600L;
}
