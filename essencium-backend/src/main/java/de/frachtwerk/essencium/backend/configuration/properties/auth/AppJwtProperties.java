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

package de.frachtwerk.essencium.backend.configuration.properties.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * JWT and session/token lifetime configuration bound from the {@code app.auth.jwt.*} namespace.
 *
 * <p>These properties control how access tokens, refresh tokens and API tokens are issued and
 * validated, as well as how expired session tokens are cleaned up. All durations are expressed in
 * seconds.
 *
 * <p>Correlations: {@link #accessTokenExpiration} should be shorter than {@link
 * #refreshTokenExpiration} (a short-lived access token is renewed via the longer-lived refresh
 * token, whose value is also used as the refresh-cookie {@code Max-Age}). {@link
 * #maxSessionExpirationTime} is the cut-off used by the periodic cleanup job — session tokens older
 * than this are deleted — and {@link #cleanupInterval} determines how often that job runs.
 */
@Configuration
@ConfigurationProperties(prefix = "app.auth.jwt")
@Validated
@Getter
@Setter
public class AppJwtProperties {

  /**
   * Issuer ({@code iss}) claim written into and required when validating JWTs. Mandatory ({@link
   * NotEmpty}); has no default and must be supplied. Changing it invalidates previously issued
   * tokens.
   */
  @NotNull @NotEmpty private String issuer;

  /**
   * Lifetime of an access token in seconds. Default: {@code 900} (15 minutes). Should be
   * significantly shorter than {@link #refreshTokenExpiration}.
   */
  @Min(0)
  private int accessTokenExpiration = 900; // 15 minutes

  /**
   * Lifetime of a refresh token in seconds; also used as the {@code Max-Age} of the refresh cookie.
   * Default: {@code 2592000} (30 days).
   */
  @Min(0)
  private int refreshTokenExpiration = 2592000; // 30 days

  /**
   * Interval in seconds between runs of the scheduled session-token cleanup job. Default: {@code
   * 3600} (1 hour). Drives {@code EssenciumScheduler.sessionTokenCleanup()}, which deletes tokens
   * older than {@link #maxSessionExpirationTime}.
   */
  private int cleanupInterval = 3600; // 1 hour

  /**
   * Maximum absolute lifetime of a session (in seconds) after which its tokens are removed by the
   * cleanup job, regardless of refresh activity. Default: {@code 86400} (24 hours). Used together
   * with {@link #cleanupInterval}.
   */
  @Min(0)
  private int maxSessionExpirationTime = 86400; // 24 hours

  /**
   * Default validity of a newly created API token in seconds, used when the caller does not specify
   * one. Default: {@code 2592000} (30 days).
   */
  @Min(0)
  private int defaultApiTokenExpiration = 2592000; // 30 days
}
