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

package de.frachtwerk.essencium.backend.model.representation.assembler;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.BasicRepresentation;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.io.Serializable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiTokenAssembler
    extends AbstractRepresentationAssembler<ApiToken, ApiTokenRepresentation> {

  private final AbstractUserService<
          ? extends AbstractBaseUser<?>,
          ? extends EssenciumUserDetails<?>,
          ? extends Serializable,
          ? extends BaseUserDto<?>>
      userService;

  @Override
  public @NonNull ApiTokenRepresentation toModel(@NonNull ApiToken entity) {
    return ApiTokenRepresentation.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedAt(entity.getUpdatedAt())
        .updatedBy(entity.getUpdatedBy())
        .linkedUser(
            BasicRepresentation.from(userService.loadUserByUsername(entity.getLinkedUser())))
        .description(entity.getDescription())
        .validUntil(entity.getValidUntil())
        .status(entity.getStatus())
        .rights(entity.getRights())
        .token(entity.getToken())
        .build();
  }
}
