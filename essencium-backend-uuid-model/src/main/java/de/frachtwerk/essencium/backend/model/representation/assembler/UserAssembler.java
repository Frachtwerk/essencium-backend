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

package de.frachtwerk.essencium.backend.model.representation.assembler;

import de.frachtwerk.essencium.backend.model.User;
import de.frachtwerk.essencium.backend.model.representation.UserRepresentation;
import lombok.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class UserAssembler extends AbstractRepresentationAssembler<User, UserRepresentation> {
  @Override
  public @NonNull UserRepresentation toModel(@NonNull User entity) {
    return UserRepresentation.builder()
        .id(entity.getId())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .phone(entity.getPhone())
        .mobile(entity.getMobile())
        .email(entity.getEmail())
        .locale(entity.getLocale())
        .roles(entity.getRoles())
        .enabled(entity.isEnabled())
        .loginDisabled(entity.isLoginDisabled())
        .build();
  }
}
