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

import de.frachtwerk.essencium.backend.configuration.properties.embedded.UserRoleMapping;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Global OAuth2 / OIDC login behavior bound from the {@code app.auth.oauth.*} namespace.
 *
 * <p>These properties complement the standard client registrations in {@link
 * de.frachtwerk.essencium.backend.configuration.properties.OAuth2ClientRegistrationProperties}.
 * They split into two groups: values that are always global and non-overridable ({@link #enabled}
 * and the redirect settings), and role/signup defaults that a single provider may override via its
 * {@code OAuth2ClientRegistrationProperties.ClientProvider} entry.
 *
 * <p>Correlation: {@link #defaultRedirectUrl} and {@link #failureRedirectUrl} must satisfy the
 * whitelist in {@link #allowedRedirectUrls}; {@link #allowSignup}, {@link #updateRole}, {@link
 * #userRoleAttr} and {@link #roles} are the fallback values used whenever a provider does not
 * specify its own.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.auth.oauth")
public class AppOAuth2Properties {
  // global, non-overridable properties

  /** Whether OAuth2/OIDC login is enabled. Default: {@code false}. */
  private boolean enabled;

  /**
   * URL the user is redirected to after a successful OAuth2 login. Must be allowed by {@link
   * #allowedRedirectUrls}.
   */
  private String defaultRedirectUrl;

  /**
   * URL the user is redirected to after a failed OAuth2 login. Must be allowed by {@link
   * #allowedRedirectUrls}.
   */
  private String failureRedirectUrl;

  /**
   * Whitelist of URLs (exact or wildcard patterns) permitted as post-login redirect targets.
   * Default: empty. Guards {@link #defaultRedirectUrl}/{@link #failureRedirectUrl} and any
   * client-supplied redirect target.
   */
  private List<String> allowedRedirectUrls = new ArrayList<>();

  // global properties that can be overridden by provider-specific properties

  /**
   * Global default for whether unknown users are auto-created on first OAuth2 login. Default:
   * {@code false}. Overridable per provider.
   */
  private boolean allowSignup = false;

  /**
   * Global default for whether a user's roles are refreshed from the provider on each login.
   * Default: {@code false}. Overridable per provider. Only meaningful together with {@link #roles}.
   */
  private boolean updateRole = false;

  /**
   * Global default name of the token/user-info claim that carries the user's roles/groups. Default:
   * {@code "groups"}. Overridable per provider.
   */
  private String userRoleAttr = "groups";

  /**
   * Global default mapping of provider role/group values (read from {@link #userRoleAttr}) to
   * Essencium roles. Default: empty. Overridable per provider. See {@link UserRoleMapping}.
   */
  private List<UserRoleMapping> roles = new ArrayList<>();
}
