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

package de.frachtwerk.essencium.backend.configuration.properties.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.token")
@Validated
public class AppTokenProperties {
  /**
   * IP addresses or CIDR ranges that are permitted to use API tokens. Accepts both IPv4 (e.g.
   * {@code 203.0.113.42}, {@code 10.0.0.0/8}) and IPv6 (e.g. {@code 2001:db8::/32}) notation.
   *
   * <p>When empty (the default), every IP address is allowed and the whitelist check is skipped
   * entirely. Session tokens (login / refresh) are never affected by this setting.
   *
   * <p>Must be disjoint from {@code trustedProxies}: an address listed there is always treated as
   * infrastructure and will never be checked against this list.
   */
  @NotNull private Set<@ValidIpOrCidr String> allowedIpAddresses = Set.of();

  /**
   * IP addresses or CIDR ranges of reverse proxies that sit between the internet and this
   * application. When set, the {@code X-Forwarded-For} header is evaluated: the chain of IPs is
   * walked from right to left, trusted-proxy entries are skipped, and the first non-trusted IP is
   * used as the effective client address for whitelist checks.
   *
   * <p>Must be disjoint from {@code allowedIpAddresses}: a proxy that is listed here will never be
   * treated as an allowed client.
   *
   * <p>When empty (the default), only {@code remoteAddr} is checked and {@code X-Forwarded-For} is
   * ignored entirely.
   */
  @NotNull private Set<@ValidIpOrCidr String> trustedProxies = Set.of();

  /**
   * Name of the HTTP request header that must carry one of the configured {@link #presharedSecrets}
   * when the PSK check is active.
   *
   * <p>Defaults to {@code X-API-Token-PSK}. Only evaluated when {@link #presharedSecrets} is
   * non-empty.
   */
  private String presharedSecretHeaderName = "X-API-Token-PSK";

  /**
   * Set of accepted pre-shared secret values for API token requests. When non-empty, every API
   * token request must include a header whose name is defined by {@link #presharedSecretHeaderName}
   * and whose value exactly matches one of the entries in this set.
   *
   * <p>When empty (the default), the PSK check is skipped entirely. This setting never affects
   * session tokens (login / refresh).
   */
  @NotNull private Set<@NotBlank String> presharedSecrets = Set.of();
}
