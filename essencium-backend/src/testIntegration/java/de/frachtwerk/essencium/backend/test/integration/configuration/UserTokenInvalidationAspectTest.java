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

package de.frachtwerk.essencium.backend.test.integration.configuration;

import static org.junit.jupiter.api.Assertions.*;

import de.frachtwerk.essencium.backend.configuration.UserTokenInvalidationAspect;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({"test_postgresql"})
@Import(UserTokenInvalidationAspect.class)
public class UserTokenInvalidationAspectTest {
  private final TestBaseUserRepository testBaseUserRepository;
  private final RoleRepository roleRepository;
  private final RightRepository rightRepository;

  private final ApiTokenRepository apiTokenRepository;
  private final SessionTokenRepository sessionTokenRepository;

  private final TestingUtils testingUtils;

  @Container
  static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17");

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    if (!POSTGRES_CONTAINER.isRunning()) {
      POSTGRES_CONTAINER.withMinimumRunningDuration(Duration.ofSeconds(5)).start();
    }
    registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
  }

  @Autowired
  public UserTokenInvalidationAspectTest(
      TestBaseUserRepository testBaseUserRepository,
      RoleRepository roleRepository,
      RightRepository rightRepository,
      ApiTokenRepository apiTokenRepository,
      SessionTokenRepository sessionTokenRepository,
      TestingUtils testingUtils) {
    this.testBaseUserRepository = testBaseUserRepository;
    this.roleRepository = roleRepository;
    this.rightRepository = rightRepository;
    this.apiTokenRepository = apiTokenRepository;
    this.sessionTokenRepository = sessionTokenRepository;
    this.testingUtils = testingUtils;
  }

  @BeforeEach
  public void setup() {
    apiTokenRepository.deleteAll();
    sessionTokenRepository.deleteAll();
    testingUtils.clearUsers();
    testingUtils.clearRoles();
    testingUtils.clearRights();
  }

  @Nested
  class UserModificationTests {
    @Test
    public void testUserTokenInvalidationAspect_UserCreation() {
      TestUser newUser =
          TestUser.builder()
              .email(UUID.randomUUID() + "@essencium.dev")
              .firstName("newUser")
              .lastName("testUser")
              .build();

      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      testBaseUserRepository.save(newUser);

      assertEquals(userCount + 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserCreationCollection() {
      TestUser newUser =
          TestUser.builder()
              .email(UUID.randomUUID() + "@essencium.dev")
              .firstName("newUser")
              .lastName("testUser")
              .build();

      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      testBaseUserRepository.saveAll(List.of(newUser));

      assertEquals(userCount + 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserDeletionByEntity() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testBaseUserRepository.delete(testUser);

      assertEquals(userCount - 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserDeletionByEntityCollection() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testBaseUserRepository.deleteAll(List.of(testUser));

      assertEquals(userCount - 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserDeletionById() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testBaseUserRepository.deleteById(Objects.requireNonNull(testUser.getId()));

      assertEquals(userCount - 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserDeletionByIdCollection() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testBaseUserRepository.deleteAllById(Set.of(Objects.requireNonNull(testUser.getId())));

      assertEquals(userCount - 1, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserUpdate_RelevantChanges() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testUser.setEmail("updated_" + testUser.getEmail());
      testBaseUserRepository.save(testUser);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserUpdateCollection_RelevantChanges() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testUser.setEmail("updated_" + testUser.getEmail());
      testBaseUserRepository.saveAll(Set.of(testUser));

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserUpdate_IrrelevantChanges() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testUser.setFirstName("updated_" + testUser.getFirstName());
      testBaseUserRepository.save(testUser);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_UserUpdateCollection_IrrelevantChanges() {
      TestUser testUser = testingUtils.createRandomUser();
      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      testUser.setFirstName("updated_" + testUser.getFirstName());
      testBaseUserRepository.saveAll(List.of(testUser));

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }
  }

  @Nested
  class RoleModificationTests {
    @Test
    public void testUserTokenInvalidationAspect_RoleDeletionByEntity() {
      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      String message =
          assertThrows(DataIntegrityViolationException.class, () -> roleRepository.delete(role))
              .getMessage();
      assertEquals("Role is still in use by 1 users", message);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleDeletionById() {
      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      String message =
          assertThrows(
                  DataIntegrityViolationException.class,
                  () -> roleRepository.deleteById(role.getId()))
              .getMessage();
      assertEquals("Role is still in use by 1 users", message);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleDeletionByEntityCollection() {
      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      String message =
          assertThrows(
                  DataIntegrityViolationException.class,
                  () -> roleRepository.deleteAll(Set.of(role)))
              .getMessage();
      assertEquals("Role is still in use by 1 users", message);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleDeletionByIdCollection() {
      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      String message =
          assertThrows(
                  DataIntegrityViolationException.class,
                  () -> roleRepository.deleteAllById(List.of(role.getId())))
              .getMessage();
      assertEquals("Role is still in use by 1 users", message);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleUpdate_AddingRights_ByEntity() {
      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      role.getRights().add(testingUtils.createRandomRight());
      roleRepository.save(role);

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleUpdate_AddingRights_ByEntityCollection() {

      Right right = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      role.getRights().add(testingUtils.createRandomRight());
      roleRepository.saveAll(Set.of(role));

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount, apiTokenRepository.count());
      assertEquals(sessionTokenCount, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleUpdate_RemovingRights_ByEntity() {
      Right right1 = testingUtils.createRandomRight();
      Right right2 = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right1, right2));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right1));
      assertTrue(testUser.getRights().contains(right2));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      role.getRights().remove(right2);
      Role save = roleRepository.save(role);

      assertTrue(save.getRights().contains(right1));
      assertFalse(role.getRights().contains(right2));

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }

    @Test
    public void testUserTokenInvalidationAspect_RoleUpdate_RemovingRights_ByEntityCollection() {

      Right right1 = testingUtils.createRandomRight();
      Right right2 = testingUtils.createRandomRight();
      Role role = testingUtils.createRandomRole(Set.of(right1, right2));
      TestUser testUser = testingUtils.createRandomUser(Set.of(role));

      assertTrue(testUser.getRoles().contains(role));
      assertTrue(testUser.getRights().contains(right1));
      assertTrue(testUser.getRights().contains(right2));

      testingUtils.createApiTokenForUser(testUser);
      testingUtils.createSessionTokenForUser(testUser);
      long userCount = testBaseUserRepository.count();
      long apiTokenCount = apiTokenRepository.count();
      long sessionTokenCount = sessionTokenRepository.count();

      assertEquals(3, userCount); // Including admin user and test user from profile
      assertEquals(1, apiTokenCount);
      assertEquals(2, sessionTokenCount);

      role.getRights().remove(right2);
      List<Role> roles = roleRepository.saveAll(List.of(role));

      Role save =
          roles.stream()
              .filter(r -> Objects.equals(r.getId(), role.getId()))
              .findFirst()
              .orElseThrow();
      assertTrue(save.getRights().contains(right1));
      assertFalse(role.getRights().contains(right2));

      assertEquals(userCount, testBaseUserRepository.count());
      assertEquals(apiTokenCount - 1, apiTokenRepository.count());
      assertEquals(sessionTokenCount - 2, sessionTokenRepository.count());
    }
  }

  @Nested
  class RightModificationTests {}
}
