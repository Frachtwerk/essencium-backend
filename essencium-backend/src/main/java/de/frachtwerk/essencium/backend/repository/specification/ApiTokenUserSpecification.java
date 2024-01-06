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

package de.frachtwerk.essencium.backend.repository.specification;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import net.kaczmarzyk.spring.data.jpa.domain.GreaterThanOrEqual;
import net.kaczmarzyk.spring.data.jpa.domain.In;
import net.kaczmarzyk.spring.data.jpa.domain.LessThanOrEqual;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;

@Spec(path = "id", params = "ids", paramSeparator = ',', spec = In.class)
interface ApiTokenIdsInSpec extends Specification<ApiTokenUser> {}

@Spec(path = "createdAt", params = "createdAtFrom", spec = GreaterThanOrEqual.class)
interface ApiTokenCreatedAtFromSpec extends Specification<ApiTokenUser> {}

@Spec(path = "createdAt", params = "createdAtTo", spec = LessThanOrEqual.class)
interface ApiTokenCreatedAtToSpec extends Specification<ApiTokenUser> {}

@Spec(path = "validUntil", params = "validUntilFrom", spec = GreaterThanOrEqual.class)
interface ApiTokenValidUntilFromSpec extends Specification<ApiTokenUser> {}

@Spec(path = "validUntil", params = "validUntilTo", spec = LessThanOrEqual.class)
interface ApiTokenValidUntilToSpec extends Specification<ApiTokenUser> {}

@Spec(path = "description", params = "description", spec = LikeConcatenated.class)
interface ApiTokenDescriptionSpec extends Specification<ApiTokenUser> {}

@Spec(
    path = "validUntil",
    params = "valid",
    defaultVal = "#{T(java.time.LocalDate).now()}",
    valueInSpEL = true,
    spec = GreaterThanOrEqual.class)
interface ApiTokenValidSpec extends Specification<ApiTokenUser> {}

public interface ApiTokenUserSpecification
    extends ApiTokenIdsInSpec,
        ApiTokenCreatedAtFromSpec,
        ApiTokenCreatedAtToSpec,
        ApiTokenValidUntilFromSpec,
        ApiTokenValidUntilToSpec,
        ApiTokenDescriptionSpec,
        ApiTokenValidSpec {}
