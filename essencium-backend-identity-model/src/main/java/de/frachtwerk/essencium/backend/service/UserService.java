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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.User;
import de.frachtwerk.essencium.backend.model.dto.AppUserDto;
import de.frachtwerk.essencium.backend.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbstractUserService<User, Long, AppUserDto> {

  protected UserService(
      @NotNull UserRepository userRepository,
      @NotNull PasswordEncoder passwordEncoder,
      @NotNull UserMailService userMailService,
      @NotNull RoleService roleService,
      @NotNull DefaultRoleInitializer roleInitializer,
      @NotNull JwtTokenService jwtTokenService) {
    super(userRepository, passwordEncoder, userMailService, roleService, roleInitializer, jwtTokenService);
  }

  @Override
  protected @NotNull <E extends AppUserDto> User convertDtoToEntity(@NotNull E entity) {
    Set<Role> roles =
        entity.getRoles().stream().map(roleService::getByName).collect(Collectors.toSet());
    return User.builder()
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
        .loginDisabled(entity.isLoginDisabled())
        .build();
  }

  @Override
  public AppUserDto getNewUser() {
    return new AppUserDto();
  }
}
