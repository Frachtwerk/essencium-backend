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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.ServletContext;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class RoleControllerIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestingUtils testingUtils;

  @Autowired private RoleRepository roleRepository;

  @Autowired private RightRepository rightRepository;
  @Autowired private TestBaseUserRepository userRepository;

  private String accessToken;

  private Set<Right> testRights;

  private Role testEditableRole;

  private Role testProtectedRole;

  @BeforeEach
  public void setupSingle() throws Exception {
    TestUser testUser = testingUtils.getOrCreateAdminUser();
    accessToken = testingUtils.createAccessToken(testUser, mockMvc);

    testRights =
        Set.of(
            Right.builder().authority("RIGHT_1").description("RIGHT_1").build(),
            Right.builder().authority("RIGHT_2").description("RIGHT_2").build());
    testRights = new HashSet<>(rightRepository.saveAll(testRights));

    testEditableRole =
        roleRepository.save(Role.builder().name("TEST_ROLE_1").rights(testRights).build());

    testProtectedRole =
        roleRepository.save(
            Role.builder().name("TEST_ROLE_2").isProtected(true).rights(testRights).build());

    assertNotNull(testEditableRole);
    assertNotNull(testProtectedRole);
    assertNotNull(testRights);
    assertThat(testRights).hasSize(2);
  }

  @AfterEach
  public void tearDownSingle() {
    assertNotNull(testEditableRole);
    assertNotNull(testProtectedRole);
    assertNotNull(testRights);
    roleRepository.delete(testEditableRole);
    roleRepository.delete(testProtectedRole);
    rightRepository.deleteAll(testRights);
  }

  @Test
  void checkRoleControllerExistence() {
    ServletContext servletContext = webApplicationContext.getServletContext();

    assertThat(servletContext).isNotNull().isInstanceOf(MockServletContext.class);
    assertThat(webApplicationContext.getBean("roleController")).isNotNull();
  }

  @Test
  void testPatchEditableRoleFail() throws Exception {
    final var testNewName = "NEW_TEST_ROLE_1";
    final var testNewDescription = "NEW_TEST_DESCRIPTION_1";
    final var testRoleUpdate =
        Map.of(
            "name", testNewName,
            "description", testNewDescription);
    final var testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            patch("/v1/roles/{id}", testEditableRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ResourceUpdateException))
        .andExpect(
            result ->
                assertEquals("Name cannot be updated", result.getResolvedException().getMessage()));
    // .andExpect(jsonPath("$.name", is(testNewName)))
    // .andExpect(jsonPath("$.protected", is(false)))
    // .andExpect(jsonPath("$.description", is(testNewDescription)));
  }

  @Test
  void testPatchEditableRoleSuccess() throws Exception {
    final var testNewDescription = "NEW_TEST_DESCRIPTION_1";
    final var testRoleUpdate = Map.of("description", testNewDescription);
    final var testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            patch("/v1/roles/{id}", testEditableRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(testEditableRole.getName())))
        .andExpect(jsonPath("$.protected", is(false)))
        .andExpect(jsonPath("$.description", is(testNewDescription)))
        .andExpect(jsonPath("$.rights", hasSize(2)));
  }

  @Test
  void testUpdateEditableRoleFail1() throws Exception {
    final String testNewName = "NEW_TEST_ROLE_1";
    final String testNewDescription = "NEW_TEST_DESCRIPTION_1";
    final Role testRoleUpdate =
        Role.builder().name(testNewName).description(testNewDescription).rights(Set.of()).build();

    final String testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            put("/v1/roles/{id}", testEditableRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ResourceUpdateException))
        .andExpect(
            result ->
                assertEquals(
                    "Name needs to match entity name", result.getResolvedException().getMessage()));
  }

  @Test
  void testUpdateEditableRoleSuccess() throws Exception {
    final String testNewDescription = "NEW_TEST_DESCRIPTION_1";
    final Role testRoleUpdate =
        Role.builder()
            .name(testEditableRole.getName())
            .description(testNewDescription)
            .rights(Set.of())
            .build();

    final String testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            put("/v1/roles/{id}", testEditableRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(testEditableRole.getName())))
        .andExpect(jsonPath("$.description", is(testNewDescription)))
        .andExpect(jsonPath("$.rights", hasSize(0)));
  }

  @Test
  void testDeleteEditableRoleNotAllowed() throws Exception {
    Role assignedRole = roleRepository.save(Role.builder().name("ASSIGNED_ROLE").build());
    // Create and Save User with assignedRole
    testingUtils.createUser("test.user@testuser.com", "TEST", "USER", assignedRole);

    final var roleCountBefore = roleRepository.count();
    final var rightCountBefore = rightRepository.count();
    final var userCountBefore = userRepository.count();

    mockMvc
        .perform(
            delete("/v1/roles/" + assignedRole.getName())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isForbidden());

    final var roleCountAfter = roleRepository.count();
    final var rightCountAfter = rightRepository.count();
    final var userCountAfter = userRepository.count();

    assertEquals(roleCountBefore, roleCountAfter);
    assertEquals(rightCountBefore, rightCountAfter); // right shall not be deleted
    assertEquals(userCountBefore, userCountAfter);
  }

  @Test
  void testDeleteEditableRole() throws Exception {
    final var roleCountBefore = roleRepository.count();
    final var rightCountBefore = rightRepository.count();
    final var userCountBefore = userRepository.count();

    mockMvc
        .perform(
            delete("/v1/roles/" + testEditableRole.getName())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isNoContent());

    final var roleCountAfter = roleRepository.count();
    final var rightCountAfter = rightRepository.count();
    final var userCountAfter = userRepository.count();

    assertEquals(roleCountBefore - 1, roleCountAfter);
    assertEquals(rightCountBefore, rightCountAfter); // right shall not be deleted
    assertEquals(userCountBefore, userCountAfter);
  }

  @Test
  void testPatchProtectedRole() throws Exception {
    final var testRoleUpdate =
        Map.of(
            "name", "test",
            "description", "test");
    final var testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            patch("/v1/roles/" + testProtectedRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isForbidden());
  }

  @Test
  void testUpdateProtectedRole() throws Exception {
    final var testRoleUpdate =
        Role.builder()
            .name(testProtectedRole.getName())
            .description("test")
            .rights(Set.of())
            .build();

    final var testRoleUpdateJson = objectMapper.writeValueAsString(testRoleUpdate);

    mockMvc
        .perform(
            put("/v1/roles/" + testProtectedRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .content(testRoleUpdateJson))
        .andExpect(status().isForbidden());
  }

  @Test
  void testDeleteProtectedRole() throws Exception {
    final var roleCountBefore = roleRepository.count();
    final var rightCountBefore = rightRepository.count();

    mockMvc
        .perform(
            delete("/v1/roles/" + testProtectedRole.getName())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isForbidden());

    final var roleCountAfter = roleRepository.count();
    final var rightCountAfter = rightRepository.count();

    assertEquals(roleCountBefore, roleCountAfter);
    assertEquals(rightCountBefore, rightCountAfter);
  }
}
