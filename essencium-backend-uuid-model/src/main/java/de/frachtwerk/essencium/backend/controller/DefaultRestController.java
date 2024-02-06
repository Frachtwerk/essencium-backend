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

import de.frachtwerk.essencium.backend.model.Identifiable;
import de.frachtwerk.essencium.backend.model.UUIDModel;
import de.frachtwerk.essencium.backend.service.DefaultAssemblingEntityService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

public class DefaultRestController<
        M extends UUIDModel, I extends Identifiable<UUID>, O, S extends Specification<M>>
    extends AbstractAccessAwareController<M, UUID, I, O, S> {
  private final DefaultAssemblingEntityService<M, I, O> abstractAssemblingEntityService;

  public DefaultRestController(DefaultAssemblingEntityService<M, I, O> service) {
    super(service);
    this.abstractAssemblingEntityService = service;
  }

  @Override
  public O toRepresentation(M entity) {
    return abstractAssemblingEntityService.toOutput(entity);
  }

  @Override
  public Page<O> toRepresentation(Page<M> page) {
    return abstractAssemblingEntityService.toOutput(page);
  }
}
