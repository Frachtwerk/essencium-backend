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

package de.frachtwerk.essencium.backend.configuration.properties;

import java.net.URI;
import java.nio.file.Path;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sentry")
@Data
public class SentryConfigProperties {

  private static final String ENDPOINT_USER_FEEDBACK = "/user-feedback/";

  private String apiUrl;
  private String organization;
  private String project;
  private String token;

  private URI baseUrl() {
    return URI.create(apiUrl + Path.of("projects", organization, project));
  }

  public URI userFeedback() {
    return URI.create(baseUrl() + ENDPOINT_USER_FEEDBACK);
  }

  public boolean isValid() {
    return Strings.isNotEmpty(apiUrl)
        && Strings.isNotEmpty(organization)
        && Strings.isNotEmpty(project)
        && Strings.isNotEmpty(token);
  }
}
