/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.test.integration.model;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Ensures that @BatchSize on User.roles, Role.rights, and ApiToken.rights minimizes SQL queries
 * during paginated loading, avoiding the N+1 problem.
 */
@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test_h2")
class BatchSizeIntegrationTest {

  private static final int BATCH_COUNT = 10;

  private final TestingUtils testingUtils;
  private final TestBaseUserRepository userRepository;
  private final RoleRepository roleRepository;
  private final ApiTokenRepository apiTokenRepository;
  private final EntityManager entityManager;

  private Statistics statistics;

  @Autowired
  BatchSizeIntegrationTest(
      TestingUtils testingUtils,
      TestBaseUserRepository userRepository,
      RoleRepository roleRepository,
      ApiTokenRepository apiTokenRepository,
      EntityManager entityManager) {
    this.testingUtils = testingUtils;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.apiTokenRepository = apiTokenRepository;
    this.entityManager = entityManager;
  }

  @BeforeEach
  void setUp() {
    SessionFactory sessionFactory =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
    sessionFactory.getStatistics().setStatisticsEnabled(true);
    statistics = sessionFactory.getStatistics();

    Right sharedRight = testingUtils.createRandomRight();
    for (int i = 0; i < BATCH_COUNT; i++) {
      Role role = testingUtils.createRandomRole(Set.of(sharedRight));
      TestUser user = testingUtils.createRandomUser(Set.of(role));
      testingUtils.createApiTokenForUser(user);
    }

    entityManager.clear();
    statistics.clear();
  }

  @AfterEach
  void tearDown() {
    apiTokenRepository.deleteAll();
    testingUtils.clearUsers();
    testingUtils.clearRoles();
    testingUtils.clearRights();
    statistics.setStatisticsEnabled(false);
  }

  @Test
  void loadingUserPageShouldBatchRolesAndRights() {
    List<TestUser> users = userRepository.findAll(PageRequest.of(0, 20)).getContent();
    assertThat(users).hasSizeGreaterThanOrEqualTo(BATCH_COUNT);

    for (TestUser user : users) {
      assertThat(user.getRoles()).isNotEmpty();
      // Access the collection so any fetch SQL is executed and counted by Hibernate statistics;
      // content is not asserted.
      user.getRoles().forEach(role -> role.getRights().size());
    }

    assertBatched(statistics.getPrepareStatementCount(), users.size(), "users");
  }

  @Test
  void loadingRolePageShouldBatchRights() {
    List<Role> roles = roleRepository.findAll(PageRequest.of(0, 20)).getContent();
    assertThat(roles).hasSizeGreaterThanOrEqualTo(BATCH_COUNT);

    // Access the collection so any fetch SQL is executed and counted by Hibernate statistics;
    // content is not asserted.
    roles.forEach(role -> role.getRights().size());

    assertBatched(statistics.getPrepareStatementCount(), roles.size(), "roles");
  }

  @Test
  void loadingApiTokenPageShouldBatchRights() {
    List<ApiToken> tokens = apiTokenRepository.findAll(PageRequest.of(0, 20)).getContent();
    assertThat(tokens).hasSizeGreaterThanOrEqualTo(BATCH_COUNT);

    // Access the collection so any fetch SQL is executed and counted by Hibernate statistics;
    // content is not asserted.
    tokens.forEach(token -> token.getRights().size());

    assertBatched(statistics.getPrepareStatementCount(), tokens.size(), "api tokens");
  }

  private void assertBatched(long queryCount, int entityCount, String entityLabel) {
    // findAll(Pageable) baseline: 1 content query + 1 count query + relationship loading.
    // Without @BatchSize: N relationship queries (one per entity).
    // With @BatchSize(20): relationship loading collapses into a few batched queries.
    // The assertion keeps the total well below the number of loaded entities.
    assertThat(queryCount)
        .as(
            "Expected batched queries for %s (far less than %d), but got %d.",
            entityLabel, entityCount, queryCount)
        .isLessThan(entityCount);
  }
}
