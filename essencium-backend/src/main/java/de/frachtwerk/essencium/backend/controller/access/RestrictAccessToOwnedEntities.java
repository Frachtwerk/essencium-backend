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

package de.frachtwerk.essencium.backend.controller.access;

import de.frachtwerk.essencium.backend.controller.AbstractAccessAwareController;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * This annotation can be used on REST controller methods, REST controller types or Entity
 * (database-wise) types served by the REST controller. If it is to be used on the latter the
 * serving REST controller needs to annotated with {@link ExposesEntity}. If multiple locations are
 * annotated simultaneously methods take precedence over REST controller types, which take
 * precedence over Entity types.
 *
 * <p>This annotation specifies if access to certain objects of an Entity type should be restricted
 * based on the concept of 'owning' this object (see {@link OwnershipSpec}). The owning criterion is
 * defined similar to a {@link Spec} annotation, to be applied as a filter for database queries.
 *
 * <p>That a user should only have access to an object if she 'owns' it is specified by specifying
 * certain {@link Role Roles} or {@link Right Rights}. If the user has the specified {@link Role} or
 * one of the specified {@link Right Rights} the restriction is applied.
 *
 * <p>The restriction can only be applied to REST controller methods that have a {@link
 * Specification} parameter. The restriction is conjunct with the given {@link Specification} while
 * resolving REST call parameters. The resolved {@link Specification} object can then be used inside
 * the controller method to do the actual access check. For single entities this can be accomplished
 * e.g. by calling {@link BaseRepository#exists(Specification)} or {@link
 * AbstractEntityService#existsFiltered(Specification)} respectively. Multiple entities can be
 * filtered directly e.g. by calling {@link JpaSpecificationExecutor#findAll(Specification)} or
 * {@link AbstractEntityService#getAllFiltered(Specification)} respectively.
 *
 * <p>A convenient way of using this annotation is in conjunction with {@link
 * AbstractAccessAwareController}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictAccessToOwnedEntities {
  /** The {@link Role} names to apply the restriction for. */
  String[] roles() default {};

  /** The {@link Right} names to apply the restriction for. */
  String[] rights() default {};
}
