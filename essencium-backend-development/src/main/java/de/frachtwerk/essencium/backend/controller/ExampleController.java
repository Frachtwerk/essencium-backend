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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

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

  @Override
  @GetMapping
  @Secured("EXAMPLE_READ")
  public Page<ExampleRepresentation> findAll(
      ExampleSpecification specification, Pageable pageable) {
    return super.findAll(specification, pageable);
  }

  @Override
  @GetMapping("/{id}")
  @Secured("EXAMPLE_READ")
  public ExampleRepresentation findById(
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    return super.findById(exampleSpecification);
  }

  @Override
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Secured("EXAMPLE_CREATE")
  public ExampleRepresentation create(ExampleEntityDto exampleEntityDto) {
    return super.create(exampleEntityDto);
  }

  @Override
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Secured("EXAMPLE_UPDATE")
  public ExampleRepresentation update(
      Long aLong,
      ExampleEntityDto exampleEntityDto,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    return super.update(aLong, exampleEntityDto, exampleSpecification);
  }

  @Override
  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Secured("EXAMPLE_UPDATE")
  public ExampleRepresentation update(
      Long aLong,
      Map<String, Object> userFields,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    return super.update(aLong, userFields, exampleSpecification);
  }

  @Override
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Secured("EXAMPLE_DELETE")
  public void delete(
      Long aLong,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    super.delete(aLong, exampleSpecification);
  }
}
