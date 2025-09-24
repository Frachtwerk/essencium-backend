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

package de.frachtwerk.essencium.backend.test.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class AuthCompareTest {

  private final WebApplicationContext webApplicationContext;
  private final MockMvc mockMvc;
  private final TestingUtils testingUtils;

  @Autowired
  AuthCompareTest(
      WebApplicationContext webApplicationContext, MockMvc mockMvc, TestingUtils testingUtils) {
    this.webApplicationContext = webApplicationContext;
    this.mockMvc = mockMvc;
    this.testingUtils = testingUtils;
  }

  @BeforeEach
  void setupSingle() {
    testingUtils.clearUsers();
  }

  @Test
  void checkUserControllerExistence() {
    ServletContext servletContext = webApplicationContext.getServletContext();

    assertThat(servletContext).isNotNull().isInstanceOf(MockServletContext.class);
    assertThat(webApplicationContext.getBean("testUserController")).isNotNull();
  }

  @Test
  void testCreateAccesToken() {
    final var testUser = testingUtils.createUser(testingUtils.getRandomUser());

    assertThatNoException().isThrownBy(() -> testingUtils.createAccessToken(testUser, mockMvc));
  }
}
