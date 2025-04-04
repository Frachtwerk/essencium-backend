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

package de.frachtwerk.essencium.backend.test.integration.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.configuration.properties.UserProperties;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.service.TestUserService;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Service
public class TestingUtils {

  public static final String ADMIN_PASSWORD = "adminAdminAdmin";
  public static final String DEFAULT_PASSWORD = "userUserUser";

  private static TestUser adminUser = null;

  private final RightRepository rightRepository;
  private final RoleRepository roleRepository;
  private final TestUserService userService;
  private final InitProperties initProperties;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final Set<Long> registry = new HashSet<>();

  @Autowired
  public TestingUtils(
      @NotNull final RightRepository rightRepository,
      @NotNull final RoleRepository roleRepository,
      @NotNull final TestUserService userService,
      @NotNull final InitProperties initProperties) {
    this.rightRepository = rightRepository;
    this.roleRepository = roleRepository;
    this.userService = userService;
    this.initProperties = initProperties;
  }

  @NotNull
  public TestUser createAdminUser() {
    adminUser = userService.loadUserByUsername("devnull@frachtwerk.de");
    Objects.requireNonNull(
        adminUser, "The admin user in application-local_integration_test.yaml need to be set");
    return adminUser;
  }

  @NotNull
  public TestUser getOrCreateAdminUser() {
    if (adminUser == null) {
      return createAdminUser();
    }
    return adminUser;
  }

  @NotNull
  public TestUser createRandomUser() {
    return createUser(getRandomUser());
  }

  @NotNull
  public TestUser createUser(@Nullable final String username, @NotNull Role role) {
    return createUser(username, "User", "Admin", role);
  }

  public TestUser createUser(
      @Nullable final String username,
      @Nullable final String firstName,
      @Nullable final String lastName,
      @NotNull Role role) {
    final String sanitizedUsername =
        Objects.requireNonNullElseGet(username, TestingUtils::randomUsername);
    TestUserDto user = new TestUserDto();
    user.setEnabled(true);
    user.setEmail(sanitizedUsername);
    user.setPassword(DEFAULT_PASSWORD);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.getRoles().add(role.getName());

    return createUser(user);
  }

  public TestUser createUser(TestUserDto user) {
    final TestUser createdUser = userService.create(user);
    registry.add(createdUser.getId());
    return createdUser;
  }

  public TestUserDto getRandomUser() {
    return TestUserDto.builder()
        .email(randomUsername())
        .enabled(true)
        .password(DEFAULT_PASSWORD)
        .firstName(RandomStringUtils.randomAlphabetic(5, 10))
        .lastName(RandomStringUtils.randomAlphabetic(5, 10))
        .roles(Set.of(createRandomRole().getName()))
        .locale(Locale.GERMAN)
        .build();
  }

  public Role createRandomRole() {
    Role role = new Role();
    role.setName("Random-Role-" + OffsetDateTime.now().toInstant().toEpochMilli());
    role.setRights(Collections.emptySet());
    return roleRepository.save(role);
  }

  public Role createOrGetAdminRole() {
    Role adminRole = roleRepository.findByName("ADMIN");
    Objects.requireNonNull(
        adminRole, "The admin role in application-local_integration_test.yaml need to be set");
    return adminRole;
  }

  public String createAdminAccessToken(MockMvc mockMvc) throws Exception {
    return createAccessToken(getOrCreateAdminUser(), mockMvc, ADMIN_PASSWORD);
  }

  public String createAccessToken(TestUser testUser, MockMvc mockMvc) throws Exception {
    return createAccessToken(testUser, mockMvc, DEFAULT_PASSWORD);
  }

  /**
   * Deletes all users that had previously been created by methods of this class.
   *
   * <p>Note: We can't just do deleteAll(), because some tests rely on the 'devnull@frachtwerk.de'
   * user that is created as part of the default initialization.
   */
  public void clearUsers() {
    final boolean[] firedOnce = {false};
    List<String> initUsers =
        initProperties.getUsers().stream().map(UserProperties::getUsername).toList();
    userService.getAll().stream()
        .filter(user -> !initUsers.contains(user.getEmail()))
        .forEach(
            user -> {
              try {
                userService.deleteById(user.getId());
                registry.remove(user.getId());
              } catch (ResourceNotFoundException ignored) {
              } catch (NotAllowedException exception) {
                // expected once, when we try to delete ourself
                if (!firedOnce[0]) {
                  firedOnce[0] = true;
                } else {
                  throw exception;
                }
              }
            });
    adminUser = null;
  }

  public static String strongPassword() {
    return "nowMakeThisAStrongP4ssword:)";
  }

  private static String randomUsername() {
    return RandomStringUtils.randomAlphanumeric(5, 10) + "@frachtwerk.de";
  }

  public SecurityContext getSecurityContextMock(TestUser returnedUser) {
    SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
    Authentication authenticationMock = Mockito.mock(Authentication.class);

    Mockito.when(securityContextMock.getAuthentication()).thenReturn(authenticationMock);
    Mockito.when(authenticationMock.getPrincipal()).thenReturn(returnedUser);
    return securityContextMock;
  }

  public String createAccessToken(TestUser testUser, MockMvc mockMvc, String password)
      throws Exception {
    LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), password);
    String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

    ResultActions result =
        mockMvc
            .perform(
                post("/auth/token")
                    .header("user-agent", "JUnit")
                    .content(loginRequestJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    String resultString = result.andReturn().getResponse().getContentAsString();
    JsonNode responseJson = objectMapper.readTree(resultString);
    return responseJson.get("token").asText();
  }
}
