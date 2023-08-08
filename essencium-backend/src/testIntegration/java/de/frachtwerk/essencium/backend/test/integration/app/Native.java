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

package de.frachtwerk.essencium.backend.test.integration.app;

import de.frachtwerk.essencium.backend.controller.access.OwnershipSpec;
import de.frachtwerk.essencium.backend.controller.access.RestrictAccessToOwnedEntities;
import de.frachtwerk.essencium.backend.model.NativeIdModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@RestrictAccessToOwnedEntities(roles = {"AdminRole", "Test"})
@OwnershipSpec.Conjunction(
    value =
        @OwnershipSpec.Or({
          @OwnershipSpec(path = "createdBy", userAttribute = "email"),
          @OwnershipSpec(path = "prop", constVal = NativeController.OWNED_BY_ALL_VALUE)
        }),
    and =
        @OwnershipSpec(
            path = "f.name",
            constVal = "meins",
            joins = @Join(path = "foreign", alias = "f")))
public class Native extends NativeIdModel {
  private String prop;

  @ManyToOne(cascade = CascadeType.MERGE)
  private Foreign foreign;
}
