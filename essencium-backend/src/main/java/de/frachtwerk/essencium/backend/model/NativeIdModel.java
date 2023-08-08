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

package de.frachtwerk.essencium.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @deprecated Due to breaking changes in Hibernate, the previous option to let the database
 *     determine the choice of id sequence is no longer available. The 'native' strategy has been
 *     modified so that it is still compatible with existing PostgreSQL-based projects. For new
 *     projects, the classes <a href="./SequenceIdModel">SequenceIdModel</a> and <a
 *     href="./SequenceIdModel">IdentityIdModel</a> should be used.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@Deprecated
public class NativeIdModel extends AbstractBaseModel<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
