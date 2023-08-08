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

package de.frachtwerk.essencium.backend.configuration.initialization;

import java.util.List;

/**
 * Provides a list of {@link DataInitializer}s to insert fixtures into the database on first
 * startup. Default initializers are used, if no custom ones are given. To provide custom
 * initializers and override default, add a bean to your application that extends this class and is
 * annotated as @Primary
 */
@FunctionalInterface
public interface DataInitializationConfiguration {

  List<DataInitializer> getInitializers();
}
