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

package de.frachtwerk.essencium.backend.model.representation;

import com.fasterxml.jackson.annotation.JsonFilter;
import de.frachtwerk.essencium.backend.controller.access.JsonAllowFor;
import de.frachtwerk.essencium.backend.controller.advice.AccessAwareJsonViewAdvice;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonFilter(AccessAwareJsonViewAdvice.FILTER_NAME)
public class ExampleRepresentation extends ModelRepresentation {

  private String content;

  @JsonAllowFor(rights = {BasicApplicationRight.Authority.API_DEVELOPER})
  private String contentVisibleWithRightApiDeveloper;

  @JsonAllowFor(roles = {"ADMIN"})
  private String contentVisibleWithRoleAdmin;
}
