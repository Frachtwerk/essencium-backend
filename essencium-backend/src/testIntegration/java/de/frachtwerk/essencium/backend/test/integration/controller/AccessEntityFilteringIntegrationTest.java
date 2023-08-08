/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.app.*;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
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
class AccessEntityFilteringIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private TestingUtils testingUtils;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private NativeService service;

  @Autowired private ForeignService foreignService;

  private String accessToken;

  @Autowired private RightRepository rightRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private TestBaseUserRepository userRepository;

  @Autowired private NativeRepository nativeRepository;

  private final Queue<Right> createdRights = new LinkedBlockingQueue<>();
  private final Queue<Role> createdRoles = new LinkedBlockingQueue<>();
  private final Queue<TestUser> createdUsers = new LinkedBlockingQueue<>();

  @BeforeEach
  public void setupSingle() throws Exception {
    TestUser testUser = testingUtils.getOrCreateAdminUser();
    accessToken = testingUtils.createAccessToken(testUser, mockMvc);
  }

  @AfterEach
  public void tearDown() {
    while (!createdUsers.isEmpty()) {
      userRepository.delete(createdUsers.poll());
    }
    while (!createdRoles.isEmpty()) {
      roleRepository.delete(createdRoles.poll());
    }
    while (!createdRights.isEmpty()) {
      rightRepository.delete(createdRights.poll());
    }
    nativeRepository.deleteAll();
  }

  @Test
  void testHidingNotOwnedEntities() throws Exception {
    createEntities();

    mockMvc
        .perform(get("/v1/native").header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].prop", Matchers.is(NativeController.OWNED_BY_ALL_VALUE)))
        .andExpect(jsonPath("$.content[1].prop", is("Ein zweiter Wert")))
        .andExpect(jsonPath("$.content[2].prop", is(NativeController.OWNED_BY_ALL_VALUE)));
  }

  @Test
  void testShowAll() throws Exception {
    createEntities();

    Role r =
        roleRepository.save(
            Role.builder().name("Test").rights(Set.copyOf(rightRepository.findAll())).build());
    createdRoles.add(r);
    final TestUser user = testingUtils.createUser("test@test.de", r);
    createdUsers.add(user);
    final String token = testingUtils.createAccessToken(user, mockMvc);

    mockMvc
        .perform(get("/v1/native").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)))
        .andExpect(jsonPath("$.content[0].prop", is(NativeController.OWNED_BY_ALL_VALUE)))
        .andExpect(jsonPath("$.content[1].prop", is("Ein zweiter Wert")))
        .andExpect(jsonPath("$.content[2].prop", is(NativeController.OWNED_BY_ALL_VALUE)))
        .andExpect(jsonPath("$.content[3].prop", is("Ein anderer Wert")));
  }

  @Test
  void testRestrictedRight() throws Exception {
    createEntities();

    final Right right = rightRepository.save(Right.builder().authority("READ_OWN").build());
    createdRights.add(right);
    Role r =
        roleRepository.save(
            Role.builder().name("Test").rights(Set.copyOf(rightRepository.findAll())).build());
    createdRoles.add(r);
    final TestUser user = testingUtils.createUser("test2@test.de", r);
    createdUsers.add(user);
    final String token = testingUtils.createAccessToken(user, mockMvc);

    createOwnedByAll(token);

    mockMvc
        .perform(get("/v1/native/restricted").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].prop", is(NativeController.OWNED_BY_ALL_VALUE)));
  }

  @Test
  void testSpecInParameter() throws Exception {
    createEntities();

    mockMvc
        .perform(
            get("/v1/native2/withSpec")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void testAnnotationOnType() throws Exception {
    createEntities();

    Role r =
        roleRepository.save(
            Role.builder().name("Test").rights(Set.copyOf(rightRepository.findAll())).build());
    createdRoles.add(r);
    final TestUser user = testingUtils.createUser("test@test.de", r);
    createdUsers.add(user);
    final String token = testingUtils.createAccessToken(user, mockMvc);

    final NativeDTO inputDto = new NativeDTO();
    inputDto.setProp(NativeController.OWNED_BY_ALL_VALUE);
    final Foreign foreign = foreignService.create(new Foreign("meins"));
    inputDto.setForeignId(foreign.getId());
    service.create(inputDto);

    mockMvc
        .perform(get("/v1/native2").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].prop", is(NativeController.OWNED_BY_ALL_VALUE)));
  }

  private void createEntities() throws Exception {
    createOwnedByAll(this.accessToken);

    mockMvc
        .perform(
            post("/v1/native")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("prop", "Ein zweiter Wert"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.prop", is("Ein zweiter Wert")))
        .andExpect(jsonPath("$.id", Matchers.greaterThan(0)));

    final NativeDTO inputDto = new NativeDTO();
    inputDto.setProp(NativeController.OWNED_BY_ALL_VALUE);
    service.create(inputDto);

    final NativeDTO hidden = new NativeDTO();
    hidden.setProp("Ein anderer Wert");
    service.create(hidden);
  }

  private void createOwnedByAll(String accessToken) throws Exception {
    final Map<String, String> input = Map.of("prop", NativeController.OWNED_BY_ALL_VALUE);
    final String inputJson = objectMapper.writeValueAsString(input);

    mockMvc
        .perform(
            post("/v1/native")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.prop", is(NativeController.OWNED_BY_ALL_VALUE)))
        .andExpect(jsonPath("$.id", Matchers.greaterThan(0)));
  }
}
