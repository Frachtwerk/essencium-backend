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

package de.frachtwerk.essencium.backend.test.integration.controller;

import de.frachtwerk.essencium.backend.controller.AbstractUserController;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.assembler.TestUserAssembler;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.service.TestUserService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestUserController
    extends AbstractUserController<
        TestUser, TestUser, TestUserDto, BaseUserSpec<TestUser, Long>, Long> {

  protected TestUserController(TestUserService userService, TestUserAssembler assembler) {
    super(userService, assembler);
  }
}
