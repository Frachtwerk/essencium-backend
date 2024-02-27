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

package de.frachtwerk.essencium.backend.controller.access;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import de.frachtwerk.essencium.backend.controller.AbstractAccessAwareController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;

/**
 * This annotation can be used on REST controller methods, REST controller types or Entity
 * (database-wise) types served by the REST controller. If it is to be used on the latter the
 * serving REST controller needs to annotated with {@link ExposesEntity}. If multiple locations are
 * annotated simultaneously methods take precedence over REST controller types, which take
 * precedence over Entity types.
 *
 * <p>This annotation specifies the ownership criterion that is tested if a user calling a REST
 * endpoint has a specific right or role (see {@link RestrictAccessToOwnedEntities}). The owning
 * criterion is defined similar to a {@link Spec} annotation, to be applied as a filter for database
 * queries.
 *
 * <p>A convenient way of using this annotation is in conjunction with {@link
 * AbstractAccessAwareController}.
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface OwnershipSpec {
  /** The database path selecting the desired value of the entity for the Ownership check. */
  String path();

  /**
   * A constant value that is checked against the value specified in path. If set this value is used
   * instead of the one specified in userAttribute.
   */
  String[] constVal() default {};

  boolean valueInSpEL() default false;

  /**
   * The database path selecting the desired value of the calling user for the Ownership check.
   *
   * <p>Per default the user id is used.
   */
  String userAttribute() default "id";

  /**
   * The predicate operator checking the entity value against a user attribute or constant value.
   *
   * <p>Per default the {@link Equal} operator is used.
   */
  Class<? extends Specification> spec() default Equal.class;

  /** {@link Join Joins} that are needed to select the desired value of the entity. */
  Join[] joins() default {};

  @Retention(RUNTIME)
  @Target({TYPE, METHOD})
  @interface And {
    OwnershipSpec[] value();
  }

  @Retention(RUNTIME)
  @Target({TYPE, METHOD})
  @interface Or {
    OwnershipSpec[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD})
  @interface Disjunction {
    And[] value();

    OwnershipSpec[] or() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD})
  @interface Conjunction {
    Or[] value();

    OwnershipSpec[] and() default {};
  }
}
