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

import java.net.URI;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry integration properties bound from the {@code sentry.*} namespace.
 *
 * <p>These values let the backend act as a proxy to the Sentry REST API — primarily for forwarding
 * user feedback (see {@link #userFeedback()}). All four fields are required for the integration to
 * work; {@link #isValid()} reports whether the configuration is complete, and the {@code
 * SentryProxyController} (see {@link EssenciumOverrideProperties#isSentryProxyController()}) is
 * only useful when it is.
 */
@Configuration
@ConfigurationProperties(prefix = "sentry")
@Data
public class SentryProperties {

  private static final String ENDPOINT_USER_FEEDBACK = "/user-feedback/";

  /**
   * Base URL of the Sentry REST API, e.g. {@code https://sentry.io/api/0/}. Combined with {@link
   * #organization} and {@link #project} to build the project endpoint. No default.
   */
  private String apiUrl;

  /** Sentry organization slug the {@link #project} belongs to. No default. */
  private String organization;

  /** Sentry project slug that feedback/events are sent to. No default. */
  private String project;

  /** Authentication token used as a bearer credential against the Sentry API. No default. */
  private String token;

  /**
   * Builds the Sentry project base URL from {@link #apiUrl}, {@link #organization} and {@link
   * #project}.
   */
  private URI baseUrl() {
    final String base = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
    return URI.create(base + "projects/" + organization + "/" + project);
  }

  /** Returns the fully qualified Sentry user-feedback endpoint for the configured project. */
  public URI userFeedback() {
    return URI.create(baseUrl() + ENDPOINT_USER_FEEDBACK);
  }

  /**
   * Returns {@code true} only when all four properties are non-empty and the integration is usable.
   */
  public boolean isValid() {
    return Strings.isNotEmpty(apiUrl)
        && Strings.isNotEmpty(organization)
        && Strings.isNotEmpty(project)
        && Strings.isNotEmpty(token);
  }
}
