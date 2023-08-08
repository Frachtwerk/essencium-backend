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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.ExampleEntity;
import de.frachtwerk.essencium.backend.model.dto.ExampleEntityDto;
import de.frachtwerk.essencium.backend.model.representation.ExampleRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.ExampleAssembler;
import de.frachtwerk.essencium.backend.repository.ExampleRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

@Service
public class ExampleService
    extends DefaultAssemblingEntityService<ExampleEntity, ExampleEntityDto, ExampleRepresentation> {

  protected ExampleService(@NotNull ExampleRepository repository, ExampleAssembler assembler) {
    super(repository, assembler);
  }

  @Override
  protected @NotNull <E extends ExampleEntityDto> ExampleEntity convertDtoToEntity(
      @NotNull E entity) {
    return ExampleEntity.builder().id(entity.getId()).content(entity.getContent()).build();
  }
}
