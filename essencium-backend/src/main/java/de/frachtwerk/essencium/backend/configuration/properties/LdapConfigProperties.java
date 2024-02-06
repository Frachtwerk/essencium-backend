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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.auth.ldap")
public class LdapConfigProperties extends FeatureToggleProperties {

  private String url;
  private String userSearchBase;
  private String userSearchFilter;
  private String groupSearchBase;
  private String groupSearchFilter;
  private String groupRoleAttribute = "spring.security.ldap.dn";
  private String defaultRole = "USER";
  private String managerDn;
  private String managerPassword;
  private String userFirstnameAttr = "notSet";
  private String userLastnameAttr = "notSet";
  private boolean allowSignup;
  private boolean updateRole;
  private List<UserRoleMapping> roles = new ArrayList<>();
}
