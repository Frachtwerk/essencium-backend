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

import de.frachtwerk.essencium.backend.configuration.properties.UserRoleMapping;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.security.oauth2.client")
public class OAuth2ClientRegistrationProperties {

  private Map<String, Registration> registration;
  private Map<String, ClientProvider> provider;

  @EqualsAndHashCode
  @Data
  @Builder
  public static class Registration {
    private String provider;
    private String clientId;
    private String clientSecret;
    private String clientAuthenticationMethod;
    private String authorizationGrantType;
    private String redirectUri;
    private Set<String> scope;
    private String clientName;
    private String imageUrl;
    private ClientRegistrationAttributes attributes;
  }

  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientProvider {
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String userInfoAuthenticationMethod;
    private String userNameAttribute;
    private String jwkSetUri;
    private String issuerUri;
    // override global properties
    private Boolean allowSignup;
    private Boolean updateRole;
    private String userRoleAttr;
    private List<UserRoleMapping> roles;
  }

  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientRegistrationAttributes {
    private String username;
    private String firstname;
    private String lastname;
    private String name;
  }
}
