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
import de.frachtwerk.essencium.backend.model.representation.BasicRepresentation;
import de.frachtwerk.essencium.backend.model.representation.ExampleRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.ExampleSpecification;
import de.frachtwerk.essencium.backend.service.ExampleService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
      @Parameter(hidden = true) ExampleSpecification specification, Pageable pageable) {
    return super.findAll(specification, pageable);
  }

  @Override
  @GetMapping("/all")
  @Secured("EXAMPLE_READ")
  public List<BasicRepresentation> findAll(
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification specification) {
    return super.findAll(specification);
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
  @Secured("EXAMPLE_CREATE")
  public ExampleRepresentation create(@RequestBody ExampleEntityDto exampleEntityDto) {
    return super.create(exampleEntityDto);
  }

  @Override
  @PutMapping("/{id}")
  @Secured("EXAMPLE_UPDATE")
  public ExampleRepresentation update(
      @PathVariable("id") Long aLong,
      @RequestBody ExampleEntityDto exampleEntityDto,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    return super.update(aLong, exampleEntityDto, exampleSpecification);
  }

  @Override
  @PatchMapping("/{id}")
  @Secured("EXAMPLE_UPDATE")
  public ExampleRepresentation update(
      @PathVariable("id") Long aLong,
      @RequestBody Map<String, Object> userFields,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    return super.update(aLong, userFields, exampleSpecification);
  }

  @Override
  @DeleteMapping("/{id}")
  @Secured("EXAMPLE_DELETE")
  public void delete(
      @PathVariable("id") Long aLong,
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class)
          ExampleSpecification exampleSpecification) {
    super.delete(aLong, exampleSpecification);
  }
}
