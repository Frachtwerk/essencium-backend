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

package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.service.RoleService;
import org.assertj.core.api.AbstractAssert;

public class RoleServiceAssert extends AbstractAssert<RoleServiceAssert, RoleService> {
  protected RoleServiceAssert(RoleService actual) {
    super(actual, RoleServiceAssert.class);
  }

  public void invokedNeverGetByNameFor(String roleName) {
    invokedGetByNameNTimesFor(0, roleName);
  }

  public void invokedGetByNameNTimesFor(int invokedTimes, String roleName) {
    verify(actual, times(invokedTimes)).getByName(roleName);
  }
}
