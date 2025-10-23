package de.frachtwerk.essencium.backend.test.integration.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.frachtwerk.essencium.backend.configuration.UserTokenInvalidationAspect;
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
}
