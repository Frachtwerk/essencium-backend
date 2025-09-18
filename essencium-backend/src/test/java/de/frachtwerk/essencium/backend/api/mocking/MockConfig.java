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

package de.frachtwerk.essencium.backend.api.mocking;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.io.Serializable;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The MockConfig utility class can be used as entry point to configure often used mocked services
 * in a centralized manner. To chain the configuration of several mocks, the {@link
 * MockConfig#givenMocks(MockConfiguration)} and {@link MockConfig#and(MockConfiguration)} can be
 * used. Every configuration of a mocked service needs to implement the {@link MockConfiguration}
 * interface to chain the configurations.
 */
public class MockConfig {

  public static <I extends Serializable> UserRepositoryMockConfiguration<I> configure(
      BaseUserRepository<?, I> baseRepository) {

    return new UserRepositoryMockConfiguration<>(baseRepository);
  }

  public static <I extends Serializable> BaseRepositoryMockConfiguration<I> configure(
      BaseRepository<?, I> baseRepository) {

    return new BaseRepositoryMockConfiguration<>(baseRepository);
  }

  public static PasswordEncoderMockConfiguration configure(PasswordEncoder passwordEncoder) {

    return new PasswordEncoderMockConfiguration(passwordEncoder);
  }

  public static RoleServiceMockConfiguration configure(RoleService roleService) {

    return new RoleServiceMockConfiguration(roleService);
  }

  public static MailServiceMockConfiguration configure(UserMailService userMailService) {

    return new MailServiceMockConfiguration(userMailService);
  }

  public static JwtTokenServiceMockConfiguration configure(JwtTokenService jwtTokenService) {

    return new JwtTokenServiceMockConfiguration(jwtTokenService);
  }

  public static MockConfig givenMocks(MockConfiguration configuration) {
    return new MockConfig();
  }

  public MockConfig and(MockConfiguration configuration) {
    return this;
  }
}
