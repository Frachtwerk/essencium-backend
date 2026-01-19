/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.RoleService;

public class RoleServiceMockConfiguration implements MockConfiguration {

  private final RoleService mockedObject;

  public RoleServiceMockConfiguration(RoleService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public RoleServiceMockConfiguration returnDefaultRoleOnDefaultRoleCall() {
    doReturn(TestObjects.roles().defaultRole()).when(mockedObject).getDefaultRole();

    return this;
  }

  public RoleServiceMockConfiguration returnRoleOnGetByNameFor(Role returnValue) {
    doReturn(returnValue).when(mockedObject).getByName(returnValue.getName());

    return this;
  }
}
