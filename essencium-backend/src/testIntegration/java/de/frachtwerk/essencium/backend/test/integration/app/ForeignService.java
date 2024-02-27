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

package de.frachtwerk.essencium.backend.test.integration.app;

import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ForeignService extends AbstractEntityService<Foreign, Long, Foreign> {
  @Autowired
  public ForeignService(@NotNull ForeignTypeRepository repository) {
    super(repository);
  }

  @Override
  protected <E extends Foreign> @NotNull Foreign convertDtoToEntity(@NotNull E entity) {
    final Foreign nat = new Foreign(entity.getName());
    nat.setId(entity.getId());
    return nat;
  }
}
