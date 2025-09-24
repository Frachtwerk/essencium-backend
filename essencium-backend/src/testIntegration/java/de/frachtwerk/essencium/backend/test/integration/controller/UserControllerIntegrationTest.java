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

import static de.frachtwerk.essencium.backend.test.integration.util.TestingUtils.ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletContext;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class UserControllerIntegrationTest {

  private final WebApplicationContext webApplicationContext;

  private final MockMvc mockMvc;

  private final ObjectMapper objectMapper;

  private final TestBaseUserRepository userRepository;

  private final JwtTokenService jwtTokenService;

  private final TestingUtils testingUtils;

  private TestUser randomUser;

  private String accessTokenAdmin;

  private String accessTokenRandomUser;

  @Autowired
  UserControllerIntegrationTest(
      WebApplicationContext webApplicationContext,
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      TestBaseUserRepository userRepository,
      JwtTokenService jwtTokenService,
      TestingUtils testingUtils) {
    this.webApplicationContext = webApplicationContext;
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.userRepository = userRepository;
    this.jwtTokenService = jwtTokenService;
    this.testingUtils = testingUtils;
  }

  @BeforeEach
  public void setupSingle() throws Exception {
    testingUtils.clearUsers();
    randomUser = testingUtils.createUser(testingUtils.getRandomUser());
    accessTokenAdmin =
        testingUtils.createAccessToken(testingUtils.createAdminUser(), mockMvc, ADMIN_PASSWORD);
    accessTokenRandomUser = testingUtils.createAccessToken(randomUser, mockMvc);
  }

  @Test
  void checkUserControllerExistence() {
    ServletContext servletContext = webApplicationContext.getServletContext();

    assertThat(servletContext).isNotNull().isInstanceOf(MockServletContext.class);
    assertThat(webApplicationContext.getBean("testUserController")).isNotNull();
  }

  @Test
  void checkUserControllerGetById() throws Exception {
    TestUser testUser = randomUser;
    ResultActions resultActions =
        mockMvc
            .perform(
                get("/v1/users/" + testUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + this.accessTokenAdmin))
            .andExpect(status().isOk());

    String response = resultActions.andReturn().getResponse().getContentAsString();
    Optional<TestUser> result = objectMapper.readValue(response, new TypeReference<>() {});

    assertThat(result).isPresent();
    assertThat(result.get().getFirstName()).isEqualTo(testUser.getFirstName());
    assertThat(result.get().getLastName()).isEqualTo(testUser.getLastName());
    assertThat(result.get().getPassword()).isNull();
    assertThat(result.get().getEmail()).isEqualTo(testUser.getEmail());
    assertThat(result.get().isAccountNonLocked()).isTrue();
    assertThat(result.get().getUsername()).isEqualTo(testUser.getUsername());
    assertThat(result.get().getRoles()).containsAll(testUser.getRoles());
  }

  @Test
  void checkUserControllerFilterByName() throws Exception {
    TestUser testUser =
        testingUtils.createUser(
            "checkUserControllerFilterByRole@frachtwerk.de",
            "John",
            "Doe",
            testingUtils.createRandomRole());
    mockMvc
        .perform(
            get("/v1/users")
                .param("name", testUser.getFirstName())
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].id").value(is(Math.toIntExact(testUser.getId()))))
        .andExpect(jsonPath("$.content[0].firstName").value(is(testUser.getFirstName())))
        .andExpect(jsonPath("$.content[0].lastName").value(is(testUser.getLastName())));

    mockMvc
        .perform(
            get("/v1/users")
                .param("name", "something else")
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(0)))
        .andExpect(jsonPath("$.content", Matchers.empty()));
  }

  @Test
  void checkUserControllerFilterByNameBasic() throws Exception {
    TestUser testUser =
        testingUtils.createUser(
            "checkUserControllerFilterByRole@frachtwerk.de",
            "John",
            "Doe",
            testingUtils.createRandomRole());
    mockMvc
        .perform(
            get("/v1/users/basic")
                .param("name", testUser.getFirstName())
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(is(Math.toIntExact(testUser.getId()))))
        .andExpect(
            jsonPath("$[0].name")
                .value(is(testUser.getFirstName() + " " + testUser.getLastName())));

    mockMvc
        .perform(
            get("/v1/users/basic")
                .param("name", "something else")
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)))
        .andExpect(jsonPath("$", Matchers.empty()));
  }

  @Test
  void checkUserControllerFilterByRole() throws Exception {
    TestUser testUser = randomUser;
    mockMvc
        .perform(
            get("/v1/users")
                .param(
                    "roles",
                    testUser.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(",")))
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].id", is(testUser.getId()), Long.class));

    mockMvc
        .perform(
            get("/v1/users")
                .param(
                    "roles",
                    testUser.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(",")))
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].id", is(testUser.getId()), Long.class));

    mockMvc
        .perform(
            get("/v1/users")
                .param("roles", "something else")
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(0)))
        .andExpect(jsonPath("$.content", Matchers.empty()));
  }

  @Test
  void checkUserControllerPatch() throws Exception {
    TestUser testUser = randomUser;
    checkUserControllerFilterByRole();
    String newFirstName = "Peter";
    HashMap<String, Object> content = new HashMap<>();
    List<String> expectedRolesContent =
        new ArrayList<>(testUser.getRoles().stream().map(Role::getName).toList());
    expectedRolesContent.add("ADMIN");

    content.put("firstName", newFirstName);
    content.put("source", "notgonnahappen");
    content.put("roles", expectedRolesContent);

    mockMvc
        .perform(
            patch("/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isOk());

    Optional<TestUser> user = userRepository.findById(testUser.getId());
    assertThat(user).isPresent();
    assertThat(user.get().getFirstName()).isEqualTo(newFirstName);
    assertThat(user.get().getSource())
        .isEqualTo(testUser.getSource())
        .isNotEqualTo(content.get("source"));
    assertThat(user.get().getRoles().size()).isEqualTo(expectedRolesContent.size());
    assertThat(
            user.get().getRoles().stream()
                .allMatch(
                    postUpdateRole -> expectedRolesContent.contains(postUpdateRole.getName())))
        .isTrue();
  }

  @Test
  @DisplayName("Removing the admin role from last admin is not allowed")
  void testRemoveAdminRoleFromLastAdmin() throws Exception {
    TestUser adminUser = testingUtils.getOrCreateAdminUser();
    Role adminRole = testingUtils.createOrGetAdminRole();

    HashMap<String, Object> content = new HashMap<>();
    Set<Role> allRolesBeforeUpdate = adminUser.getRoles();
    String firstName = "dummy";

    List<String> allRolesWithoutAdmin =
        new ArrayList<>(
            allRolesBeforeUpdate.stream()
                .filter(role -> !role.equals(adminRole))
                .map(Role::getName)
                .toList());

    content.put("roles", allRolesWithoutAdmin);
    content.put("firstName", firstName);

    mockMvc
        .perform(
            patch("/v1/users/" + adminUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isForbidden());

    Optional<TestUser> user = userRepository.findById(adminUser.getId());
    assertThat(user).isPresent();
    assertThat(user.get().getRoles()).containsAll(allRolesBeforeUpdate);
    assertThat(user.get().getFirstName()).isNotEqualTo(firstName);
  }

  @Test
  @DisplayName("Removing the admin role from if another admin exists")
  void testRemoveAdminRoleIfAnotherAdminExistsSuccessfully() throws Exception {
    Role adminRole = testingUtils.createOrGetAdminRole();
    Role randomRole = testingUtils.createRandomRole();

    TestUser secondAdmin = testingUtils.createUser("admin2@frachtwerk.de", adminRole);

    HashMap<String, Object> content = new HashMap<>();
    Set<Role> rolesUpdate = secondAdmin.getRoles();
    rolesUpdate.add(randomRole);
    String firstName = "dummy";

    List<String> allRolesWithoutAdmin =
        new ArrayList<>(
            rolesUpdate.stream()
                .filter(role -> !role.equals(adminRole))
                .map(Role::getName)
                .toList());

    content.put("roles", allRolesWithoutAdmin);
    content.put("firstName", firstName);

    mockMvc
        .perform(
            patch("/v1/users/" + secondAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isOk());

    Optional<TestUser> user = userRepository.findById(secondAdmin.getId());
    assertThat(user).isPresent();
    assertThat(user.get().getRoles()).doesNotContain(adminRole);
    assertThat(user.get().getRoles()).isNotEmpty();
  }

  @Test
  void checkUserControllerUpdate() throws Exception {
    TestUser testUser = randomUser;
    Set<Role> roles = testUser.getRoles();
    String newFirstName = "Peter";
    String newLastName = "Pan";
    String newEmail = "peter.pan@test.de";
    String newMobile = "01234567889";
    String newPhone = "0123456789";

    TestUserDto content = new TestUserDto();
    content.setId(testUser.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
    content.setMobile(newMobile);
    content.setPhone(newPhone);
    content.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
    content.setSource("notgonnahappen"); // source must not be updated

    mockMvc
        .perform(
            put("/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isOk());

    Optional<TestUser> userOptional = userRepository.findById(testUser.getId());
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isEqualTo(newFirstName);
    assertThat(user.getLastName()).isEqualTo(newLastName);
    assertThat(user.getEmail()).isEqualTo(newEmail);
    assertThat(user.getMobile()).isEqualTo(newMobile);
    assertThat(user.getPhone()).isEqualTo(newPhone);
    assertThat(user.getRoles()).containsAll(roles);
    assertThat(user.getSource()).isEqualTo(testUser.getSource()).isNotEqualTo(content.getSource());
  }

  @Test
  @DisplayName("Remove the admin role from last admin is not allowed")
  void testRemoveTheAdminRoleFromLastAdminIsForbidden() throws Exception {
    TestUser adminUser = testingUtils.getOrCreateAdminUser();
    Role adminRole = testingUtils.createOrGetAdminRole();

    Set<Role> roles = adminUser.getRoles();
    String newFirstName = "Peter";
    String newLastName = "Pan";
    String newEmail = "peter.pan@test.de";
    String newMobile = "01234567889";
    String newPhone = "0123456789";

    TestUserDto content = new TestUserDto();
    content.setId(adminUser.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
    content.setMobile(newMobile);
    content.setPhone(newPhone);
    content.setRoles(
        roles.stream()
            .filter(role -> !role.equals(adminRole))
            .map(Role::getName)
            .collect(Collectors.toSet()));

    mockMvc
        .perform(
            put("/v1/users/{id}", adminUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isForbidden());

    Optional<TestUser> userOptional = userRepository.findById(adminUser.getId());
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isNotEqualTo(newFirstName);
    assertThat(user.getLastName()).isNotEqualTo(newLastName);
    assertThat(user.getEmail()).isNotEqualTo(newEmail);
    assertThat(user.getMobile()).isNotEqualTo(newMobile);
    assertThat(user.getPhone()).isNotEqualTo(newPhone);
    assertThat(user.getRoles()).containsAll(roles);
  }

  @Test
  @DisplayName("Remove the admin role from an admin is allowed, if it is not the last")
  void testRemoveTheAdminRoleFromNotLastAdminSuccessfully() throws Exception {
    Role adminRole = testingUtils.createOrGetAdminRole();

    TestUser secondAdmin = testingUtils.createUser("admin2@frachtwerk.de", adminRole);

    Set<Role> roles = secondAdmin.getRoles();
    String newFirstName = "Peter";
    String newLastName = "Pan";
    String newEmail = "peter.pan@test.de";
    String newMobile = "01234567889";
    String newPhone = "0123456789";

    TestUserDto content = new TestUserDto();
    content.setId(secondAdmin.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
    content.setMobile(newMobile);
    content.setPhone(newPhone);
    content.setRoles(
        roles.stream()
            .filter(role -> !role.equals(adminRole))
            .map(Role::getName)
            .collect(Collectors.toSet()));

    mockMvc
        .perform(
            put("/v1/users/{id}", secondAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isOk());

    Optional<TestUser> userOptional = userRepository.findById(secondAdmin.getId());
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isEqualTo(newFirstName);
    assertThat(user.getLastName()).isEqualTo(newLastName);
    assertThat(user.getEmail()).isEqualTo(newEmail);
    assertThat(user.getMobile()).isEqualTo(newMobile);
    assertThat(user.getPhone()).isEqualTo(newPhone);
    assertThat(user.getRoles()).isNotEmpty();
    assertThat(user.getRoles()).doesNotContain(adminRole);
  }

  @Test
  void checkUserControllerPatchUnauthorized() throws Exception {
    TestUser testUser = randomUser;
    String newFirstName = "Peter";

    HashMap<String, String> content = new HashMap<>();
    content.put("firstName", newFirstName);

    mockMvc
        .perform(
            patch("/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isUnauthorized());

    Optional<TestUser> userOptional = userRepository.findById(testUser.getId());
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isEqualTo(randomUser.getFirstName());
  }

  @Test
  void checkUserControllerDelete() throws Exception {
    TestUser testUser =
        TestUser.builder()
            .email("peter.pan@frachtwerk.de")
            .firstName("Peter")
            .lastName("Pan")
            .enabled(true)
            .locale(Locale.GERMANY)
            .password("password")
            .mobile("0123456789")
            .phone("0123456789")
            .roles(testingUtils.createRandomUser().getRoles())
            .build();
    userRepository.save(testUser);

    mockMvc
        .perform(
            delete("/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    Optional<TestUser> userOptional = userRepository.findById(testUser.getId());
    assertThat(userOptional).isEmpty();
  }

  @Test
  void testGetCurrentLoggedInUser() throws Exception {
    RequestBuilder requestBuilder =
        MockMvcRequestBuilders.get("/v1/users/me")
            .header("Authorization", "Bearer " + accessTokenRandomUser)
            .accept(MediaType.APPLICATION_JSON);

    mockMvc
        .perform(requestBuilder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled", is(true)))
        .andExpect(jsonPath("$.email", is(randomUser.getEmail())));
  }

  @Test
  void testUnauthorizedWhenBearerPrefixMissing() throws Exception {
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, accessTokenRandomUser)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateSelfByDto() throws Exception {
    final TestUserDto updateDto = new TestUserDto();
    updateDto.setFirstName("Elon");
    updateDto.setLastName("Musk");
    updateDto.setPhone("0123456");
    updateDto.setMobile("0976543");
    updateDto.setLocale(Locale.ITALY);
    updateDto.setEmail("not.gonna@change.this");

    final String updateJson = objectMapper.writeValueAsString(updateDto);

    mockMvc
        .perform(
            put("/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenRandomUser)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", Matchers.is(updateDto.getFirstName())))
        .andExpect(jsonPath("$.lastName", Matchers.is(updateDto.getLastName())))
        .andExpect(jsonPath("$.phone", Matchers.is(updateDto.getPhone())))
        .andExpect(jsonPath("$.mobile", Matchers.is(updateDto.getMobile())))
        .andExpect(jsonPath("$.locale", Matchers.is(updateDto.getLocale().toString())))
        .andExpect(jsonPath("$.email", Matchers.is(randomUser.getEmail())));
  }

  @Test
  void testUpdateSelfByIndividualFields() throws Exception {
    final Map<String, String> updateFields =
        Map.of(
            "firstName", "Elon",
            "email", "not.gonna@change.this");

    final String updateJson = objectMapper.writeValueAsString(updateFields);

    mockMvc
        .perform(
            patch("/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenRandomUser)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", Matchers.is(updateFields.get("firstName"))))
        .andExpect(jsonPath("$.lastName", Matchers.is(randomUser.getLastName())))
        .andExpect(jsonPath("$.email", Matchers.is(randomUser.getEmail())));
  }

  @Test
  void testUpdateSelfWithMissingProperties() throws Exception {
    final TestUserDto updateDto = new TestUserDto();
    updateDto.setFirstName("Elon"); // lastName missing

    final String updateJson = objectMapper.writeValueAsString(updateDto);

    mockMvc
        .perform(
            put("/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenRandomUser)
                .content(updateJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", hasItem("lastName must not be empty")));
  }

  @Test
  void testUpdateSelfPasswordTooWeak() throws Exception {
    final PasswordUpdateRequest dto = new PasswordUpdateRequest("a", "adminAdminAdmin");

    mockMvc
        .perform(
            put("/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", hasItem("password is too weak. Try a longer one.")));
  }

  @Test
  void testUpdateUserPassword() throws Exception {
    final ObjectMapper localOm =
        JsonMapper.builder().configure(MapperFeature.USE_ANNOTATIONS, false).build();

    TestUserDto dto = testingUtils.getRandomUser();
    TestUser localTestUser = testingUtils.createUser(dto);

    dto.setId(localTestUser.getId());
    dto.setPassword(TestingUtils.strongPassword());

    mockMvc
        .perform(
            put("/v1/users/" + localTestUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(localOm.writeValueAsString(dto)))
        .andExpect(status().isOk());
  }

  @Test
  void testUpdateUserPasswordTooWeak() throws Exception {
    final ObjectMapper localOm =
        JsonMapper.builder()
            .configure(MapperFeature.USE_ANNOTATIONS, false)
            .build(); // otherwise, 'password' field won't be serialized

    TestUserDto dto = testingUtils.getRandomUser();
    TestUser localTestUser = testingUtils.createUser(dto);

    dto.setId(localTestUser.getId());
    dto.setPassword("a");

    mockMvc
        .perform(
            put("/v1/users/" + localTestUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(localOm.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", hasItem("password is too weak. Try a longer one.")));
  }

  @Test
  void testFilterUsersByName() throws Exception {
    final TestUser user1 =
        testingUtils.createUser(
            testingUtils.getRandomUser().toBuilder().firstName("Alan").lastName("Turing").build());

    final TestUser user2 =
        testingUtils.createUser(
            testingUtils.getRandomUser().toBuilder().firstName("Steve").lastName("Jobs").build());

    // No filtering
    mockMvc
        .perform(
            get("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isOk())
        // 1. admin user created during initialization
        // 2. normal user created during initialization
        // 3. random user created for tests (see setupSingle())
        // 4. user1
        // 5. user2
        .andExpect(jsonPath("$.totalElements", Matchers.is(5)));

    // Filter by first user firstName
    mockMvc
        .perform(
            get("/v1/users")
                .queryParam("name", user1.getFirstName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", Matchers.is(1)))
        .andExpect(jsonPath("$.content.[0].id", is(user1.getId()), Long.class));

    // Filter by second user lastName
    mockMvc
        .perform(
            get("/v1/users")
                .queryParam("name", user2.getLastName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", Matchers.is(1)))
        .andExpect(jsonPath("$.content.[0].id", is(user2.getId()), Long.class));
  }

  @Nested
  @DisplayName("Filter Tests")
  class FilterTest {
    private TestUser testUser1;
    private TestUser testUser2;

    @BeforeEach
    void setUpUsers() {
      testUser1 =
          testingUtils.createUser(
              testingUtils.getRandomUser().toBuilder().email("alan.turing@tech.de").build());

      testUser2 =
          testingUtils.createUser(
              testingUtils.getRandomUser().toBuilder().email("steve.jobs@tech.de").build());
    }

    @Test
    @DisplayName("Using no filter should return all elements")
    void testNoFilter() throws Exception {
      mockMvc
          .perform(
              get("/v1/users")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          // 1. admin user created during initialization
          // 2. normal user created during initialization
          // 3. random user created for tests (see setupSingle())
          // 4. user1
          // 5. user2
          .andExpect(jsonPath("$.totalElements", Matchers.is(5)));
    }

    @Test
    @DisplayName("Using full email should return a single user matching the email")
    void testFullEmailFilter() throws Exception {
      mockMvc
          .perform(
              get("/v1/users")
                  .queryParam("email", testUser1.getEmail())
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements", Matchers.is(1)))
          .andExpect(jsonPath("$.content[0].email", is(testUser1.getEmail()), String.class));
    }

    @Test
    @DisplayName("Using almost full email should return a single user matching the partial email")
    void testPartialEmailFilter() throws Exception {
      mockMvc
          .perform(
              get("/v1/users")
                  .queryParam("email", "alan.turing")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements", Matchers.is(1)))
          .andExpect(jsonPath("$.content[0].email", is(testUser1.getEmail()), String.class));
    }

    @Test
    @DisplayName("Using common substring should return all user matching the substring")
    void testCommonSubstringEmailFilter() throws Exception {
      mockMvc
          .perform(
              get("/v1/users")
                  .queryParam("email", "tech.de")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements", Matchers.is(2)))
          .andExpect(jsonPath("$.content[0].email", is(testUser1.getEmail()), String.class))
          .andExpect(jsonPath("$.content[1].email", is(testUser2.getEmail()), String.class));
    }
  }

  @Test
  void testTerminateUserSession() throws Exception {
    mockMvc
        .perform(
            get("/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenRandomUser))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/v1/users/" + randomUser.getId() + "/terminate")
                .header("Authorization", "Bearer " + this.accessTokenAdmin))
        .andExpect(status().is2xxSuccessful());

    mockMvc
        .perform(
            get("/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenRandomUser))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testCreateUser() throws Exception {
    final TestUserDto dto = testingUtils.getRandomUser();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
  }

  @Test
  void testCreateUserWeakPassword() throws Exception {
    final ObjectMapper localOm =
        JsonMapper.builder().configure(MapperFeature.USE_ANNOTATIONS, false).build();

    final TestUserDto dto = testingUtils.getRandomUser();
    dto.setPassword("a");

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(localOm.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", hasItem("password is too weak. Try a longer one.")));
  }

  @Test
  @DisplayName("A not admin can delete himself")
  void testANotAdminCanDeleteHimself() throws Exception {
    mockMvc
        .perform(
            delete("/v1/users/" + randomUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("The last admin can not delete himself")
  void testTheLastAdminCanNotDeleteHimself() throws Exception {
    TestUser adminUser = testingUtils.getOrCreateAdminUser();

    mockMvc
        .perform(
            delete("/v1/users/" + adminUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("An admin can be deleted if at least one other exists")
  void testAnAdminDeleteSuccessfully() throws Exception {
    Role adminRole = testingUtils.createOrGetAdminRole();
    TestUser secondAdmin = testingUtils.createUser("admin2@frachtwerk.de", adminRole);

    mockMvc
        .perform(
            delete("/v1/users/" + secondAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
        .andExpect(status().isNoContent());
  }

  @Nested
  class JWTClaimTest {
    TestUser adminUser;
    String accessTokenAdmin;

    @BeforeEach
    void setUp() throws Exception {
      testingUtils.clearUsers();
      adminUser = testingUtils.createAdminUser();
      accessTokenAdmin = testingUtils.createAccessToken(adminUser, mockMvc, ADMIN_PASSWORD);

      assertThat(adminUser.getAdditionalClaims()).hasSize(6);
      assertThat(accessTokenAdmin).isNotNull();
    }

    @Test
    @DisplayName("Check additional claims in JWT")
    void testAdditionalClaimsInJWT() {
      Claims payload = jwtTokenService.verifyToken(accessTokenAdmin);

      assertThat(payload).containsEntry(TestUser.CLAIM_TEST_INTEGER, 1);
      assertThat(payload).containsEntry(TestUser.CLAIM_TEST_LONG, 2);
      assertThat(payload).containsEntry(TestUser.CLAIM_TEST_STRING, "test");
      assertThat(payload).containsEntry(TestUser.CLAIM_TEST_BOOLEAN, true);
      assertThat(payload).containsEntry(TestUser.CLAIM_TEST_DOUBLE, 3.1);
      assertThat(payload)
          .containsEntry(TestUser.CLAIM_TEST_MAP, Map.of("key1", "value1", "key2", "value2"));
      assertThat(payload.get(TestUser.CLAIM_TEST_NON_EXISTENT)).isNull();
    }

    @Test
    void testMapOfAdditionalClaims() throws Exception {
      mockMvc
          .perform(
              get("/v1/users/token-claims")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_INTEGER, is("1")))
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_LONG, is("2")))
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_STRING, is("test")))
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_BOOLEAN, is("true")))
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_DOUBLE, is("3.1")))
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_MAP).exists())
          .andExpect(jsonPath("$." + TestUser.CLAIM_TEST_NON_EXISTENT).doesNotExist())
          .andReturn();
    }
  }
}
