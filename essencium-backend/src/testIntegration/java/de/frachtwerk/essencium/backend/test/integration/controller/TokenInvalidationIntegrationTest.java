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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.configuration.UserTokenInvalidationAspect;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.TokenInvalidationService;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestBaseUserDto;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.repository.TestSessionTokenRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.ServletContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test_h2")
@Import(UserTokenInvalidationAspect.class)
class TokenInvalidationIntegrationTest {

  private final WebApplicationContext webApplicationContext;
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  private final TestingUtils testingUtils;

  private final TestBaseUserRepository userRepository;
  private final TestSessionTokenRepository sessionTokenRepository;
  private final RoleRepository roleRepository;
  private final RightRepository rightRepository;

  private TestUser randomUser;

  private String accessTokenAdmin;

  private String accessTokenRandomUser;

  @Autowired
  TokenInvalidationIntegrationTest(
      WebApplicationContext webApplicationContext,
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      TestingUtils testingUtils,
      TestBaseUserRepository userRepository,
      TestSessionTokenRepository sessionTokenRepository,
      RoleRepository roleRepository,
      RightRepository rightRepository) {
    this.webApplicationContext = webApplicationContext;
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.testingUtils = testingUtils;
    this.userRepository = userRepository;
    this.sessionTokenRepository = sessionTokenRepository;
    this.roleRepository = roleRepository;
    this.rightRepository = rightRepository;
  }

  @TestConfiguration
  public static class TestAspectConfiguration {

    @Bean
    public UserTokenInvalidationAspect userTokenInvalidationAspect(
        TokenInvalidationService tokenInvalidationService) {
      return new UserTokenInvalidationAspect(tokenInvalidationService);
    }
  }

  @BeforeEach
  public void setupSingle() throws Exception {
    testingUtils.clearUsers();
    randomUser = testingUtils.createUser(testingUtils.getRandomUser());
    TestUser adminUser = testingUtils.createAdminUser();
    accessTokenAdmin = testingUtils.createAccessToken(adminUser, mockMvc, ADMIN_PASSWORD);
    accessTokenRandomUser = testingUtils.createAccessToken(randomUser, mockMvc);
  }

  @Test
  void checkAspectIsLoaded() {
    assertThat(webApplicationContext.getBean(UserTokenInvalidationAspect.class)).isNotNull();
  }

  @Test
  void checkUserControllerExistence() {
    ServletContext servletContext = webApplicationContext.getServletContext();

    assertThat(servletContext).isNotNull().isInstanceOf(MockServletContext.class);
    assertThat(webApplicationContext.getBean("testUserController")).isNotNull();
  }

  @Test
  void checkUserControllerGet() throws Exception {
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
    List<SessionToken> sessionTokens = sessionTokenRepository.findAll();
    assertThat(sessionTokens)
        .extracting(SessionToken::getUsername)
        .contains(testUser.getUsername());
  }

  @Test
  void checkUserControllerPatch() throws Exception {
    TestUser testUser = randomUser;
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

    Optional<TestUser> user = userRepository.findById(Objects.requireNonNull(testUser.getId()));
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

    Optional<TestUser> user = userRepository.findById(Objects.requireNonNull(adminUser.getId()));
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

    Optional<TestUser> user = userRepository.findById(Objects.requireNonNull(secondAdmin.getId()));
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

    TestBaseUserDto content = new TestBaseUserDto();
    content.setId(testUser.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
    content.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
    content.setSource("notgonnahappen"); // source must not be updated

    mockMvc
        .perform(
            put("/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + this.accessTokenAdmin)
                .content(objectMapper.writeValueAsString(content)))
        .andExpect(status().isOk());

    Optional<TestUser> userOptional =
        userRepository.findById(Objects.requireNonNull(testUser.getId()));
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isEqualTo(newFirstName);
    assertThat(user.getLastName()).isEqualTo(newLastName);
    assertThat(user.getEmail()).isEqualTo(newEmail);
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

    TestBaseUserDto content = new TestBaseUserDto();
    content.setId(adminUser.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
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

    Optional<TestUser> userOptional =
        userRepository.findById(Objects.requireNonNull(adminUser.getId()));
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isNotEqualTo(newFirstName);
    assertThat(user.getLastName()).isNotEqualTo(newLastName);
    assertThat(user.getEmail()).isNotEqualTo(newEmail);
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

    TestBaseUserDto content = new TestBaseUserDto();
    content.setId(secondAdmin.getId());
    content.setFirstName(newFirstName);
    content.setLastName(newLastName);
    content.setEmail(newEmail);
    content.setEnabled(true);
    content.setLocale(Locale.GERMANY);
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

    Optional<TestUser> userOptional =
        userRepository.findById(Objects.requireNonNull(secondAdmin.getId()));
    assertThat(userOptional).isPresent();
    TestUser user = userOptional.orElseThrow();
    assertThat(user.getFirstName()).isEqualTo(newFirstName);
    assertThat(user.getLastName()).isEqualTo(newLastName);
    assertThat(user.getEmail()).isEqualTo(newEmail);
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

    Optional<TestUser> userOptional =
        userRepository.findById(Objects.requireNonNull(testUser.getId()));
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

    Optional<TestUser> userOptional =
        userRepository.findById(Objects.requireNonNull(testUser.getId()));
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
    final TestBaseUserDto updateDto = new TestBaseUserDto();
    updateDto.setFirstName("TestName");
    updateDto.setLastName("LastName");
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
        .andExpect(jsonPath("$.locale", Matchers.is(updateDto.getLocale().toString())))
        .andExpect(jsonPath("$.email", Matchers.is(randomUser.getEmail())));
    assertThat(sessionTokenRepository.findAllByUsername(randomUser.getUsername())).hasSize(0);

    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenRandomUser)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUserTokenInvalidationOnUserUpdate() throws Exception {
    // Create a user and get access token
    TestUser testUser = testingUtils.createUser(testingUtils.getRandomUser());
    String userAccessToken = testingUtils.createAccessToken(testUser, mockMvc);

    // Verify token works initially
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // Update the user to trigger the aspect
    TestBaseUserDto updateDto = new TestBaseUserDto();
    updateDto.setId(testUser.getId());
    updateDto.setFirstName("UpdatedName");
    updateDto.setLastName(testUser.getLastName());
    updateDto.setEmail("testmail@essenciumTest.de");
    updateDto.setEnabled(true);
    updateDto.setLocale(testUser.getLocale());
    updateDto.setRoles(testUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));

    mockMvc
        .perform(
            put("/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is("UpdatedName")));

    // Verify tokens were invalidated by the aspect
    assertThat(sessionTokenRepository.findAllByUsername(testUser.getUsername())).hasSize(0);

    // Verify old token no longer works
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUserTokenInvalidationOnRoleModification() throws Exception {
    // Create a test role and user with that role
    Right right1 = testingUtils.createRandomRight();
    Right right2 = testingUtils.createRandomRight();
    Role testRole = testingUtils.createRandomRole(Set.of(right1, right2));
    TestUser testUser =
        testingUtils.createUser(
            testingUtils.getRandomUser().toBuilder().roles(Set.of(testRole.getName())).build());
    String userAccessToken = testingUtils.createAccessToken(testUser, mockMvc);

    // Verify token works initially
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // Update the role to trigger the aspect
    Map<String, String> roleUpdate = Map.of("description", "Updated description");

    assertThat(sessionTokenRepository.findAllByUsername(testUser.getUsername())).hasSize(2);

    mockMvc
        .perform(
            patch("/v1/roles/" + testRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(objectMapper.writeValueAsString(roleUpdate)))
        .andExpect(status().isOk());

    // Verify tokens were not invalidated by the aspect (as description change is not relevant)
    assertThat(sessionTokenRepository.findAllByUsername(testUser.getUsername())).hasSize(2);

    // Now update the role's rights to trigger invalidation
    Map<String, List<String>> roleUpdate2 = Map.of("rights", List.of(right1.getAuthority()));

    mockMvc
        .perform(
            patch("/v1/roles/" + testRole.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenAdmin)
                .content(objectMapper.writeValueAsString(roleUpdate2)))
        .andExpect(status().isOk());

    // Verify tokens were invalidated by the aspect
    assertThat(sessionTokenRepository.findAllByUsername(testUser.getUsername())).hasSize(0);

    // Verify old token no longer works
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUserTokenInvalidationOnRightModification() throws Exception {
    // Create a test right and role with that right
    Right testRight = Right.builder().authority("TEST_RIGHT").description("Test Right").build();
    testRight = rightRepository.save(testRight);

    Role testRole = Role.builder().name("TEST_ROLE_WITH_RIGHT").rights(Set.of(testRight)).build();
    testRole = roleRepository.save(testRole);

    // Create user with the role containing the right
    TestUser testUser =
        testingUtils.createUser(
            testingUtils.getRandomUser().toBuilder().roles(Set.of(testRole.getName())).build());
    String userAccessToken = testingUtils.createAccessToken(testUser, mockMvc);

    // Verify token works initially
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // Update the right to trigger the aspect
    testRight.setDescription("Updated Right Description");
    rightRepository.save(testRight);

    // Verify tokens were not invalidated by the aspect. (as description change is not relevant)
    assertThat(sessionTokenRepository.findAllByUsername(testUser.getUsername())).hasSize(2);

    // Verify old token still works
    mockMvc
        .perform(
            get("/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
