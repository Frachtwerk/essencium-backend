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

package de.frachtwerk.essencium.backend.configuration.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kaczmarzyk.spring.data.jpa.domain.IgnoreCaseStrategy;
import net.kaczmarzyk.spring.data.jpa.utils.CharEscaper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JPA / persistence-layer tuning bound from the {@code essencium.jpa.*} namespace.
 *
 * <p>These properties influence how Hibernate maps Java identifiers to physical database table
 * names (see {@code DataNamingConfig}) and how the {@code specification-arg-resolver} library
 * builds dynamic filter queries (see {@code SpecificationArgumentsResolverConfig}). Changing any of
 * them affects the generated schema/queries, so they should be fixed for the lifetime of a database
 * and kept consistent with the actual schema.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.jpa")
public class EssenciumJpaProperties {
  /**
   * Whether camelCase logical names should be converted to {@code snake_case} physical column/table
   * names. Default: {@code false} (logical names are used as-is, apart from the prefixing/casing
   * below).
   */
  private boolean camelCaseToUnderscore = false;

  /**
   * Prefix prepended to every generated physical table name. Default: {@code "FW_"} (e.g. table
   * {@code User} becomes {@code FW_USER}). Set to an empty string to disable prefixing. Must match
   * the prefix used by the existing schema/migrations.
   */
  private String tablePrefix = "FW_";

  /**
   * Whether generated physical table names are upper-cased. Default: {@code true}. Correlates with
   * {@link #tablePrefix} (the prefix is applied around the upper-cased name) and should be aligned
   * with {@link #ignoreCaseStrategy}.
   */
  private boolean tableNamesUpperCase = true;

  /**
   * Strategy used by the specification-arg-resolver when performing case-insensitive comparisons in
   * generated filter queries. Default: {@link IgnoreCaseStrategy#DATABASE_UPPER} (the database
   * upper-cases both sides), which matches the default {@link #tableNamesUpperCase} = {@code true}.
   */
  private IgnoreCaseStrategy ignoreCaseStrategy = IgnoreCaseStrategy.DATABASE_UPPER;

  /**
   * Escaping strategy applied to special characters (e.g. SQL {@code LIKE} wildcards) in
   * dynamically built filter queries. Default: {@link CharEscaper#DISABLED} (no escaping is
   * performed).
   */
  private CharEscaper charEscaper = CharEscaper.DISABLED;
}
