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

package de.frachtwerk.essencium.backend.test.integration.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class ResetCredentialsIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private TestingUtils testingUtils;

  @Test
  void testResetCredentials() throws Exception {
    final var testUserInput = testingUtils.getRandomUser();
    final var testUser = testingUtils.createUser(testUserInput);

    RequestBuilder requestBuilder =
        MockMvcRequestBuilders.post("/v1/reset-credentials")
            .content(testUser.getEmail())
            .contentType(MediaType.TEXT_PLAIN);

    mockMvc.perform(requestBuilder).andExpect(status().isNoContent());
  }

  @Test
  void testResetCredentialsFailForNonLocalUser() throws Exception {
    final var testUserInput = testingUtils.getRandomUser();
    testUserInput.setSource("ldap");

    final var testUser = testingUtils.createUser(testUserInput);

    RequestBuilder requestBuilder =
        MockMvcRequestBuilders.post("/v1/reset-credentials")
            .content(testUser.getEmail())
            .contentType(MediaType.TEXT_PLAIN);

    mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
  }
}
