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

package de.frachtwerk.essencium.backend.api.data;

import de.frachtwerk.essencium.backend.api.data.authentication.TestObjectsAuthentication;
import de.frachtwerk.essencium.backend.api.data.pageable.TestObjectsPageable;
import de.frachtwerk.essencium.backend.api.data.role.TestObjectsRole;
import de.frachtwerk.essencium.backend.api.data.service.TestObjectService;
import de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser;

/**
 * The utility class TestObjects serves as an easy way to receive often used "given" objects in test
 * cases.
 */
public class TestObjects {

  public static TestObjectsUser users() {
    return new TestObjectsUser();
  }

  public static TestObjectService services() {
    return new TestObjectService();
  }

  public static TestObjectsRole roles() {
    return new TestObjectsRole();
  }

  public static TestObjectsAuthentication authentication() {
    return new TestObjectsAuthentication();
  }

  public static TestObjectsPageable pageable() {
    return new TestObjectsPageable();
  }
}
