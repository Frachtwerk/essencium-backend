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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import java.io.Serializable;
import net.kaczmarzyk.spring.data.jpa.domain.*;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Or;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

@Join(path = "role", alias = "r")
@Or({
  @Spec(path = "r.id", params = "role", spec = Equal.class),
  @Spec(path = "r.name", params = "role", spec = Equal.class)
})
interface RoleSpecBase<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends BaseModelSpec<USER, ID> {}

@Spec(
    path = "firstName,lastName",
    params = "name",
    paramSeparator = ' ',
    spec = LikeConcatenated.class)
interface NameSpecBase<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends BaseModelSpec<USER, ID> {}

@Spec(
    path = "email",
    params = {"email", "user", "username"},
    spec = EqualIgnoreCase.class)
interface EmailSpecBase<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends BaseModelSpec<USER, ID> {}

public interface BaseUserSpec<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends RoleSpecBase<USER, ID>, NameSpecBase<USER, ID>, EmailSpecBase<USER, ID> {}
