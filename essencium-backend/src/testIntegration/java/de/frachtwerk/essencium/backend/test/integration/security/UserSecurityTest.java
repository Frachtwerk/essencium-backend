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

package de.frachtwerk.essencium.backend.test.integration.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class UserSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private TestingUtils testingUtils;

  @Autowired private TestBaseUserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RightRepository rightRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static TestUser testUser = null;

  @BeforeEach
  public void setupSingle() {
    testingUtils.createOrGetAdminRole();
    if (testUser == null) {
      testUser = testingUtils.getOrCreateAdminUser();
    }
    testUser.getRoles().add(roleRepository.findByName("ADMIN"));
    testUser = userRepository.save(testUser);
  }

  @Test
  void checkLoginSuccessful() throws Exception {
    LoginRequest loginRequest =
        new LoginRequest(testUser.getEmail(), TestingUtils.DEFAULT_PASSWORD);
    String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

    mockMvc
        .perform(
            post("/auth/token")
                .content(loginRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-agent", "JUint")
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.token", Matchers.not(Matchers.emptyString())));
  }

  @Test
  void checkNoCrashOnNullNonce() throws Exception {
    TestUser localTestUser = testingUtils.createRandomUser();
    localTestUser.setNonce(null);
    userRepository.save(localTestUser);

    LoginRequest loginRequest =
        new LoginRequest(localTestUser.getEmail(), TestingUtils.DEFAULT_PASSWORD);
    String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

    mockMvc
        .perform(
            post("/auth/token")
                .content(loginRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-agent", "JUint")
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.token", Matchers.not(Matchers.emptyString())));
  }

  @Test
  void checkLoginUnauthorizedBadPassword() throws Exception {
    LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "invalid password");
    String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

    mockMvc
        .perform(
            post("/auth/token")
                .content(loginRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-agent", "JUint")
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isUnauthorized());
    // in addition, a ErrorResponse should be returned as a JSON
    // however, it looks like we can't easily test this:
    // https://stackoverflow.com/a/53153161/3112139
  }

  @Test
  void checkLoginUnauthorizedBadUsername() throws Exception {
    LoginRequest loginRequest = new LoginRequest("invalid username", "invalid password");
    String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

    mockMvc
        .perform(
            post("/auth/token")
                .content(loginRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-agent", "JUint")
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());
    // in addition, a ErrorResponse should be returned as a JSON
    // however, it looks like we can't easily test this:
    // https://stackoverflow.com/a/53153161/3112139
  }
}
