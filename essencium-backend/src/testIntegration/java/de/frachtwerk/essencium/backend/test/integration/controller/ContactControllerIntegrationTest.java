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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class ContactControllerIntegrationTest {

  // Note: currently, this test relies upon ethereal.email and therefore requires internet
  // connection
  // Better practice would be to spawn a fake SMTP server, similar to what is done with Wiremock in
  // other tests

  // Note: Test only check for proper response status codes, but currently do not validate sent
  // mails' content

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestingUtils testingUtils;

  private String accessToken;

  @BeforeEach
  public void setupSingle() throws Exception {
    TestUser testUser = testingUtils.getOrCreateAdminUser();
    accessToken = testingUtils.createAccessToken(testUser, mockMvc);
  }

  @Test
  void testSendAsUser() throws Exception {
    final var payload = new ContactRequestDto(null, null, getMessageSubject(), getMesssageText());

    mockMvc
        .perform(
            post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk());
  }

  @Test
  void testSendAsUserUnauthorized() throws Exception {
    final var payload = new ContactRequestDto(null, null, getMessageSubject(), getMesssageText());

    mockMvc
        .perform(
            post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken + "-invalid")
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testSendAnonymously() throws Exception {
    final var payload =
        new ContactRequestDto(
            "Horst Seehofer", "horst@frachtwerk.de", getMessageSubject(), getMesssageText());

    mockMvc
        .perform(
            post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk());
  }

  @Test
  void testSendAnonymouslyMissingSender() throws Exception {
    final var payload = new ContactRequestDto(null, null, getMessageSubject(), getMesssageText());

    mockMvc
        .perform(
            post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest());
  }

  private String getMessageSubject() {
    return String.format(
        "Integration Test – %s – %s", getClass().getSimpleName(), LocalDateTime.now());
  }

  private String getMesssageText() {
    return String.format(
        "OS: %s %s %s<br>User: %s",
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        System.getProperty("os.version"),
        System.getProperty("user.name"));
  }
}
