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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.RightService;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
public class RightInitializer extends DefaultRightInitializer {
  public RightInitializer(RightService rightService, RoleRepository roleRepository) {
    super(rightService, roleRepository);
  }

  @Override
  public Set<Right> getAdditionalApplicationRights() {
    Set<Right> rightSet = super.getAdditionalApplicationRights();

    Stream<Right> singleRights = Stream.of(new Right("READ", ""));
    Stream<Right> crudRights1 =
        getCombinedRights(Stream.of("CREATE", "READ", "UPDATE", "DELETE"), "EXAMPLE");
    Stream<Right> crudRights2 =
        getCombinedRights(
            Stream.of(
                "CREATE",
                "READ_ALL",
                "READ_OWN",
                "UPDATE_ALL",
                "UPDATE_OWN",
                "DELETE_ALL",
                "DELETE_OWN"),
            new Right("EXAMPLE_2", "TEST"));

    rightSet.addAll(
        Stream.of(singleRights, crudRights1, crudRights2)
            .flatMap(Function.identity())
            .collect(Collectors.toSet()));

    return rightSet;
  }
}
