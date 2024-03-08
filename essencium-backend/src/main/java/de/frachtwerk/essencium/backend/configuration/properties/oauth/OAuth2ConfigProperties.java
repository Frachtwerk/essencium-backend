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

package de.frachtwerk.essencium.backend.configuration.properties.oauth;

import de.frachtwerk.essencium.backend.configuration.properties.FeatureToggleProperties;
import de.frachtwerk.essencium.backend.configuration.properties.UserRoleMapping;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "app.auth.oauth")
public class OAuth2ConfigProperties extends FeatureToggleProperties {

  private boolean allowSignup;
  private boolean updateRole;
  private boolean proxyEnabled;
  private String defaultRedirectUrl;
  private String failureRedirectUrl;
  private String userRoleAttr;
  private List<String> allowedRedirectUrls = new ArrayList<>();
  private List<UserRoleMapping> roles = new ArrayList<>();
}
