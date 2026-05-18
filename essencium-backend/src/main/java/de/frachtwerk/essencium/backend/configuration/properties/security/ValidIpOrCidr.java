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

package de.frachtwerk.essencium.backend.configuration.properties.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated {@link String} is a syntactically valid IPv4/IPv6 address or CIDR
 * range accepted by Spring Security's {@code IpAddressMatcher} (e.g. {@code 203.0.113.42}, {@code
 * 10.0.0.0/8}, {@code 2001:db8::/32}).
 *
 * <p>Blank and {@code null} values are treated as invalid.
 */
@Documented
@Constraint(validatedBy = IpOrCidrValidator.class)
@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIpOrCidr {

  String message() default "must be a valid IPv4/IPv6 address or CIDR range";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
