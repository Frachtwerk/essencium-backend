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

package de.frachtwerk.essencium.backend.repository.specification;

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import java.io.Serializable;
import net.kaczmarzyk.spring.data.jpa.domain.*;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;

@Spec(path = "id", params = "ids", paramSeparator = ',', spec = In.class)
interface IdsInSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "createdBy", params = "createdBy", spec = EqualIgnoreCase.class)
interface CreatedBySpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "updatedBy", params = "updatedBy", spec = EqualIgnoreCase.class)
interface UpdatedBySpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "createdAt", params = "createdAtFrom", spec = GreaterThanOrEqual.class)
interface CreatedAtFromSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "createdAt", params = "createdAtTo", spec = LessThanOrEqual.class)
interface CreatedAtToSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "updatedAt", params = "updatedAtFrom", spec = GreaterThanOrEqual.class)
interface UpdatedAtFromSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

@Spec(path = "updatedAt", params = "updatedAtTo", spec = LessThanOrEqual.class)
interface UpdatedAtToSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends Specification<T> {}

public interface BaseModelSpec<T extends AbstractBaseModel<ID>, ID extends Serializable>
    extends IdsInSpec<T, ID>,
        CreatedBySpec<T, ID>,
        UpdatedBySpec<T, ID>,
        CreatedAtFromSpec<T, ID>,
        CreatedAtToSpec<T, ID>,
        UpdatedAtFromSpec<T, ID>,
        UpdatedAtToSpec<T, ID> {}
