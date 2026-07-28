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
 * LDAP authentication configuration bound from the {@code app.auth.ldap.*} namespace.
 *
 * <p>When {@link #enabled} is {@code true}, users can authenticate against an LDAP/Active Directory
 * server. The properties describe how to bind to the directory ({@link #url}, {@link #managerDn},
 * {@link #managerPassword}), how to locate users and their groups (the {@code *SearchBase} / {@code
 * *SearchFilter} pairs) and how directory groups translate into Essencium roles.
 *
 * <p>Correlations: {@link #groupSearchBase}/{@link #groupSearchFilter} and {@link
 * #groupSearchSubtree} together determine which groups are read; those groups are then mapped to
 * roles via {@link #roles} (falling back to {@link #defaultRole}). {@link #allowSignup} and {@link
 * #updateRole} mirror the equivalent OAuth2 flags in {@link AppOAuth2Properties} and behave
 * analogously.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.auth.ldap")
public class AppLdapProperties {
  /** Whether LDAP authentication is enabled. Default: {@code false}. */
  private boolean enabled;

  /** LDAP server URL, e.g. {@code ldap://ldap.example.com:389}. */
  private String url;

  /** Base DN under which users are searched, e.g. {@code ou=people,dc=example,dc=com}. */
  private String userSearchBase;

  /**
   * LDAP filter used to find a user, e.g. {@code (uid={0})} where {@code {0}} is the login name.
   */
  private String userSearchFilter;

  /** Base DN under which the user's groups are searched. */
  private String groupSearchBase;

  /** LDAP filter used to find groups a user belongs to. */
  private String groupSearchFilter;

  /**
   * Whether the group search descends into the sub-tree of {@link #groupSearchBase}. Default:
   * {@code false} (only the immediate level is searched).
   */
  private boolean groupSearchSubtree = false;

  /**
   * Attribute of a group entry used as the role name for mapping. Default: {@code
   * "spring.security.ldap.dn"} (the group's full DN is used).
   */
  private String groupRoleAttribute = "spring.security.ldap.dn";

  /**
   * Role assigned to authenticated users when no {@link #roles} mapping matches their groups.
   * Default: {@code "USER"}.
   */
  private String defaultRole = "USER";

  /** DN of the manager/service account used to bind to the directory for searches. */
  private String managerDn;

  /** Password of the manager/service account referenced by {@link #managerDn}. */
  private String managerPassword;

  /**
   * Name of the LDAP attribute holding the user's first name. Default: {@code "notSet"} (no value
   * is imported until configured).
   */
  private String userFirstnameAttr = "notSet";

  /**
   * Name of the LDAP attribute holding the user's last name. Default: {@code "notSet"} (no value is
   * imported until configured).
   */
  private String userLastnameAttr = "notSet";

  /**
   * Whether users unknown in the local database are auto-created on first successful LDAP login.
   * Default: {@code false}.
   */
  private boolean allowSignup;

  /**
   * Whether the local user's roles are refreshed from the mapped LDAP groups on each login.
   * Default: {@code false}. Only meaningful together with {@link #roles}.
   */
  private boolean updateRole;

  /**
   * Mapping of LDAP group values (per {@link #groupRoleAttribute}) to Essencium roles. Default:
   * empty (all users fall back to {@link #defaultRole}). See {@link UserRoleMapping}.
   */
  private List<UserRoleMapping> roles = new ArrayList<>();
}
