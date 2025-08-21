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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {
  @NotBlank private String domain;
  @NotBlank private String url;

  /**
   * Default URL to redirect to after logout. This url must be whitelisted in
   * `allowedLogoutRedirectUrls`. This url can be overridden by the `redirectUrl` parameter in the
   * logout request. If the `redirectUrl` parameter is not provided, this url will be used.
   */
  @NotBlank private String defaultLogoutRedirectUrl;

  /**
   * List of allowed URLs to redirect to after logout. This list can contain exact URLs or wildcards
   * (e.g., `https://example.com/*`). If a URL matches any of the patterns in this list, it is
   * considered valid for redirection after logout.
   */
  @NotEmpty private List<String> allowedLogoutRedirectUrls = new ArrayList<>();
}
