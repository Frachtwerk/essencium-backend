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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SentryPropertiesTest {

  private static SentryProperties properties(
      String apiUrl, String organization, String project, String token) {
    SentryProperties sentryProperties = new SentryProperties();
    sentryProperties.setApiUrl(apiUrl);
    sentryProperties.setOrganization(organization);
    sentryProperties.setProject(project);
    sentryProperties.setToken(token);
    return sentryProperties;
  }

  private static SentryProperties validProperties() {
    return properties("https://sentry.io/api/0/", "my-org", "my-project", "secret-token");
  }

  @Nested
  class IsValid {

    @Test
    void returnsTrueWhenAllFieldsAreSet() {
      assertThat(validProperties().isValid()).isTrue();
    }

    @Test
    void returnsFalseWhenApiUrlIsNull() {
      assertThat(properties(null, "org", "project", "token").isValid()).isFalse();
    }

    @Test
    void returnsFalseWhenApiUrlIsEmpty() {
      assertThat(properties("", "org", "project", "token").isValid()).isFalse();
    }

    @Test
    void returnsFalseWhenOrganizationIsNull() {
      assertThat(properties("https://sentry.io/api/0/", null, "project", "token").isValid())
          .isFalse();
    }

    @Test
    void returnsFalseWhenOrganizationIsEmpty() {
      assertThat(properties("https://sentry.io/api/0/", "", "project", "token").isValid())
          .isFalse();
    }

    @Test
    void returnsFalseWhenProjectIsNull() {
      assertThat(properties("https://sentry.io/api/0/", "org", null, "token").isValid()).isFalse();
    }

    @Test
    void returnsFalseWhenProjectIsEmpty() {
      assertThat(properties("https://sentry.io/api/0/", "org", "", "token").isValid()).isFalse();
    }

    @Test
    void returnsFalseWhenTokenIsNull() {
      assertThat(properties("https://sentry.io/api/0/", "org", "project", null).isValid())
          .isFalse();
    }

    @Test
    void returnsFalseWhenTokenIsEmpty() {
      assertThat(properties("https://sentry.io/api/0/", "org", "project", "").isValid()).isFalse();
    }

    @Test
    void returnsFalseWhenAllFieldsAreNull() {
      assertThat(new SentryProperties().isValid()).isFalse();
    }
  }

  @Nested
  class UserFeedback {

    @Test
    void buildsEndpointWhenApiUrlHasTrailingSlash() {
      URI uri = validProperties().userFeedback();

      assertThat(uri)
          .hasToString("https://sentry.io/api/0/projects/my-org/my-project/user-feedback/");
    }

    @Test
    void buildsEndpointWhenApiUrlHasNoTrailingSlash() {
      SentryProperties sentryProperties =
          properties("https://sentry.io/api/0", "my-org", "my-project", "token");

      URI uri = sentryProperties.userFeedback();

      assertThat(uri)
          .hasToString("https://sentry.io/api/0/projects/my-org/my-project/user-feedback/");
    }

    @Test
    void trailingAndNonTrailingApiUrlProduceIdenticalEndpoints() {
      URI withSlash =
          properties("https://sentry.io/api/0/", "org", "project", "token").userFeedback();
      URI withoutSlash =
          properties("https://sentry.io/api/0", "org", "project", "token").userFeedback();

      assertThat(withSlash).isEqualTo(withoutSlash);
    }

    @Test
    void incorporatesOrganizationAndProjectSlugs() {
      URI uri =
          properties("https://sentry.io/api/0/", "acme", "checkout-service", "token")
              .userFeedback();

      assertThat(uri.toString())
          .contains("/projects/acme/checkout-service/")
          .endsWith("/user-feedback/");
    }

    @Test
    void returnsNullWhenApiUrlIsMissing() {
      SentryProperties sentryProperties = properties(null, "org", "project", "token");

      assertThat(sentryProperties.userFeedback()).isNull();
    }

    @Test
    void returnsNullWhenConfigurationIsEmpty() {
      assertThat(new SentryProperties().userFeedback()).isNull();
    }
  }
}
