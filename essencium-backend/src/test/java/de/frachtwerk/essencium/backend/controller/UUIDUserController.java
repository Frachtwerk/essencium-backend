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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.assembler.UUIDUserAssembler;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.service.UUIDUserService;
import java.util.UUID;

public class UUIDUserController
    extends AbstractUserController<
        TestUUIDUser, TestUUIDUser, UserDto<UUID>, BaseUserSpec<TestUUIDUser, UUID>, UUID> {

  protected UUIDUserController(UUIDUserService userService, UUIDUserAssembler assembler) {
    super(userService, assembler);
  }
}
