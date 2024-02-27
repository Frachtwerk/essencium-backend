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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.UUIDModel;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.util.UUID;
import lombok.Getter;

public abstract class DefaultAssemblingEntityService<M extends UUIDModel, I, R>
    extends AbstractEntityService<M, UUID, I> implements AssemblingService<M, R> {

  @Getter private final AbstractRepresentationAssembler<M, R> assembler;

  protected DefaultAssemblingEntityService(
      final BaseRepository<M, UUID> repository,
      final AbstractRepresentationAssembler<M, R> assembler) {
    super(repository);
    this.assembler = assembler;
  }
}
