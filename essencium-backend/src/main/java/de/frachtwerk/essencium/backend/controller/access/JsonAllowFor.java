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

import de.frachtwerk.essencium.backend.controller.advice.AccessAwareJsonViewAdvice;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Ownable;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used on fields or getters to restrict serialization of this property in
 * controller methods. It will only be serialized in a response body for a {@link AbstractBaseUser}
 * calling a certain controller method that has one of the specified {@link Role Roles} or {@link
 * Right Rights}. Additionally, if the containing Type is implementing the {@link Ownable}
 * interface, it will be serialized if the user is the owner of this object ( {@link
 * Ownable#isOwnedBy(AbstractBaseUser)} returns true).
 *
 * <p>Serialization restriction only works if the containing type is annotated with @JsonFilter(
 * {@link AccessAwareJsonViewAdvice#FILTER_NAME}).
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAllowFor {
  /** The {@link Role} names that allow serialization of this property. */
  String[] roles() default {};

  /** The {@link Right} names that allow serialization of this property. */
  String[] rights() default {};

  /**
   * Whether the property should be serialized for User who 'own' the specific object ( {@link
   * Ownable#isOwnedBy(AbstractBaseUser)} returns true).
   *
   * <p>True by default. Only works if containing type is implementing {@link Ownable}.
   */
  boolean allowForOwner() default true;
}
