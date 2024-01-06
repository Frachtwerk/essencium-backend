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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LongUserService extends AbstractUserService<TestLongUser, Long, UserDto<Long>> {

  protected <T extends RoleService> LongUserService(
      @NotNull BaseUserRepository<TestLongUser, Long> userRepository,
      @NotNull PasswordEncoder passwordEncoder,
      @NotNull UserMailService userMailService,
      @NotNull T roleService,
      @NotNull JwtTokenService jwtTokenService) {
    super(userRepository, passwordEncoder, userMailService, roleService, jwtTokenService);
  }

  @Override
  protected @NotNull <E extends UserDto<Long>> TestLongUser convertDtoToEntity(@NotNull E entity) {
    HashSet<Role> roles =
        entity.getRoles().stream()
            .map(roleService::getByName)
            .collect(Collectors.toCollection(HashSet::new));
    return TestLongUser.builder()
        .email(entity.getEmail())
        .enabled(entity.isEnabled())
        .roles(roles)
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .locale(entity.getLocale())
        .mobile(entity.getMobile())
        .phone(entity.getPhone())
        .source(entity.getSource())
        .id(entity.getId())
        .build();
  }

  @Override
  public UserDto<Long> getNewUser() {
    return new UserDto<>();
  }
}
