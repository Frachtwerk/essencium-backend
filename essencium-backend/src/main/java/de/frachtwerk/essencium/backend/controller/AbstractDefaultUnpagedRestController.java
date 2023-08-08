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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

public abstract class AbstractDefaultUnpagedRestController<
        I, ID extends Serializable, O extends AbstractBaseModel<ID>>
    extends AbstractRestController<I, ID, O> {

  public AbstractDefaultUnpagedRestController(AbstractEntityService<O, ID, I> service) {
    super(service);
  }

  @GetMapping
  @NotNull
  public List<O> findAll() {
    return service.getAll();
  }
}
