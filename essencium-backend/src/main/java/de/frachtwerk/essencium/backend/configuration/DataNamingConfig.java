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

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DataNamingConfig {

  @Value("${essencium-backend.jpa.table-prefix:}")
  public String prefix = "";

  @Bean
  @ConditionalOnProperty(
      value = "essencium-backend.jpa.camel-case-to-underscore",
      havingValue = "true")
  public CamelCaseToUnderscoresNamingStrategy caseSensitivePhysicalNamingStrategy() {
    return new CamelCaseToUnderscoresNamingStrategy() {

      @Override
      protected boolean isCaseInsensitive(JdbcEnvironment jdbcEnvironment) {
        return true;
      }

      @Override
      public Identifier toPhysicalTableName(
          Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return Identifier.toIdentifier(
            prefix
                + super.toPhysicalTableName(logicalName, jdbcEnvironment).getText().toUpperCase(),
            true);
      }
    };
  }

  @Bean
  @ConditionalOnProperty(
      value = "essencium-backend.jpa.camel-case-to-underscore",
      havingValue = "false",
      matchIfMissing = true)
  public PhysicalNamingStrategyStandardImpl physicalNamingStrategyStandardImpl() {
    return new PhysicalNamingStrategyStandardImpl() {
      @Override
      public Identifier toPhysicalTableName(
          Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return Identifier.toIdentifier(
            prefix
                + super.toPhysicalTableName(logicalName, jdbcEnvironment).getText().toUpperCase(),
            true);
      }
    };
  }
}
