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

package de.frachtwerk.essencium.backend.test.integration.model;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class TestUser extends AbstractBaseUser<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /*
  For Long as Primary id change code to:
  ```java
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(
      name = "hibernate_sequence",
      sequenceName = "hibernate_sequence",
      allocationSize = 1)
      private Long id;
  ```
   */
}
