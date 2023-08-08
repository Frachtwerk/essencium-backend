/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.configuration.properties.LdapConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.AuthenticationSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

@Configuration
@ConditionalOnProperty(value = "app.auth.ldap.enabled", havingValue = "true")
public class LdapContextConfig {

  private final LdapConfigProperties ldapConfigProperties;

  @Autowired
  public LdapContextConfig(LdapConfigProperties ldapConfigProperties) {
    this.ldapConfigProperties = ldapConfigProperties;
  }

  @Primary
  @Bean
  public BaseLdapPathContextSource contextSource() {
    final var contextSource = new DefaultSpringSecurityContextSource(ldapConfigProperties.getUrl());
    contextSource.setUserDn(ldapConfigProperties.getManagerDn());
    contextSource.setPassword(ldapConfigProperties.getManagerPassword());
    contextSource.setCacheEnvironmentProperties(false);
    contextSource.setAuthenticationSource(
        new AuthenticationSource() {
          @Override
          public String getPrincipal() {
            return contextSource.getUserDn();
          }

          @Override
          public String getCredentials() {
            return contextSource.getPassword();
          }
        });

    return contextSource;
  }
}
