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

package de.frachtwerk.essencium.backend.api.data.service;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.service.AdminRightRoleCache;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestObjectService {
  public UserServiceStub defaultUserService(
      @NotNull BaseUserRepository<UserStub, Long> userRepositoryMock,
      @NotNull PasswordEncoder passwordEncoderMock,
      @NotNull UserMailService userMailServiceMock,
      @NotNull RoleService roleServiceMock,
      @NotNull AdminRightRoleCache adminRightRoleCache,
      @NotNull JwtTokenService jwtTokenServiceMock) {
    return new UserServiceStub(
        userRepositoryMock,
        passwordEncoderMock,
        userMailServiceMock,
        roleServiceMock,
        adminRightRoleCache,
        jwtTokenServiceMock);
  }
}
