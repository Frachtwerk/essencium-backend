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

import de.frachtwerk.essencium.backend.controller.access.ExposesEntity;
import de.frachtwerk.essencium.backend.model.ExampleEntity;
import de.frachtwerk.essencium.backend.model.dto.ExampleEntityDto;
import de.frachtwerk.essencium.backend.model.representation.ExampleRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.ExampleSpecification;
import de.frachtwerk.essencium.backend.service.ExampleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/example")
@ExposesEntity(ExampleEntity.class)
@Tag(name = "ExampleController", description = "Example for implementing openAPI")
public class ExampleController
    extends DefaultRestController<
        ExampleEntity, ExampleEntityDto, ExampleRepresentation, ExampleSpecification> {

  public ExampleController(ExampleService service) {
    super(service);
  }
}
