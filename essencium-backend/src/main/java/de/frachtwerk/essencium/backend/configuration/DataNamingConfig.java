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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumJpaProperties;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class DataNamingConfig {

  private final EssenciumJpaProperties essenciumJpaProperties;

  @Bean
  @ConditionalOnBooleanProperty(
      value = "essencium.jpa.camel-case-to-underscore",
      havingValue = true,
      matchIfMissing = true)
  public PhysicalNamingStrategySnakeCaseImpl caseSensitivePhysicalNamingStrategy() {
    return new PhysicalNamingStrategySnakeCaseImpl() {
      @Override
      public Identifier toPhysicalTableName(
          Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        if (essenciumJpaProperties.isTableNamesUpperCase()) {
          return Identifier.toIdentifier(
              essenciumJpaProperties.getTablePrefix()
                  + super.toPhysicalTableName(logicalName, jdbcEnvironment).getText().toUpperCase(),
              true);
        } else {
          return Identifier.toIdentifier(
              essenciumJpaProperties.getTablePrefix()
                  + super.toPhysicalTableName(logicalName, jdbcEnvironment).getText(),
              true);
        }
      }
    };
  }
}
