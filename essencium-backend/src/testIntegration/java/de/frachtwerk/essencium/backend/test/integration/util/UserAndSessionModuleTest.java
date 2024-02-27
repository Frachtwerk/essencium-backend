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

package de.frachtwerk.essencium.backend.test.integration.util;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.controller.TestUserController;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.service.TestUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class UserAndSessionModuleTest {

  @Autowired(required = false)
  private TestUserController userController;

  @Autowired(required = false)
  private TestUserService userService;

  @Autowired(required = false)
  private TestBaseUserRepository userRepository;

  @Test
  void onlySessionModuleIsLoaded() {
    assertThat(userController).isNotNull();
    assertThat(userService).isNotNull();
    assertThat(userRepository).isNotNull();
  }
}
