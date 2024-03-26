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

import static de.frachtwerk.essencium.backend.service.UserEmailChangeService.E_MAIL_TOKEN_VALIDITY_IN_MONTHS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.dto.EmailVerificationRequest;
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

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class ResetCredentialsIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private TestingUtils testingUtils;

  @Autowired private ObjectMapper objectMapper;

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

  @Test
  void testValidateEmail() throws Exception {
    final var testUserInput = testingUtils.getRandomUser();
    var testUser = testingUtils.createUser(testUserInput);

    final var newEmail = "new@mail.de";

    testUser = testingUtils.updateUserToUnverifiedEmail(testUser, newEmail);

    EmailVerificationRequest verificationRequest =
        new EmailVerificationRequest(testUser.getEmailVerifyToken());

    RequestBuilder requestBuilder =
        MockMvcRequestBuilders.post("/v1/verify-email")
            .content(objectMapper.writeValueAsString(verificationRequest))
            .contentType(MediaType.APPLICATION_JSON);

    mockMvc.perform(requestBuilder).andExpect(status().isNoContent());
  }
}
