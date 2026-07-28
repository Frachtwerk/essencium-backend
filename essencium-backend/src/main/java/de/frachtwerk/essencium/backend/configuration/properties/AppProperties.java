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

/**
 * Core application-wide properties bound from the {@code app.*} namespace.
 *
 * <p>These values describe how the running instance identifies and exposes itself (domain and base
 * URL) and constrain where users may be redirected after logout. They are consumed throughout the
 * backend, e.g. when building absolute links in e-mails ({@link MailProperties}) and when
 * validating logout / redirect targets.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {
  /**
   * Public domain (host) under which this application is reachable, e.g. {@code app.example.com}.
   * Used for cookie and link generation. Mandatory ({@link NotBlank}); has no default and must be
   * supplied.
   */
  @NotBlank private String domain;

  /**
   * Fully qualified base URL of the application, e.g. {@code https://app.example.com}. Used as the
   * root for absolute links (for instance the reset / activation links in {@link MailProperties}).
   * Mandatory ({@link NotBlank}); has no default and must be supplied. Should be consistent with
   * {@link #domain}.
   */
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
