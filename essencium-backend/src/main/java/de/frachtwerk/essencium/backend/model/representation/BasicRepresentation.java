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

package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.TitleConvention;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record BasicRepresentation(Serializable id, String name) {
  public static BasicRepresentation from(Serializable id, String name) {
    if (Objects.isNull(id) || Objects.isNull(name)) {
      return null;
    }
    return new BasicRepresentation(id, name);
  }

  public static <M extends TitleConvention<? extends Serializable>> BasicRepresentation from(
      M entity) {
    if (Objects.isNull(entity)) {
      return null;
    }
    return BasicRepresentation.from(entity.getId(), entity.getTitle());
  }

  public static <M extends TitleConvention<? extends Serializable>> List<BasicRepresentation> from(
      Collection<M> list) {
    if (Objects.isNull(list)) {
      return List.of();
    }
    return list.stream().map(BasicRepresentation::from).filter(Objects::nonNull).toList();
  }
}
