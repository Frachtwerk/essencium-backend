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

package de.frachtwerk.essencium.backend.test.integration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.ApiTokenStatus;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.util.EssenciumScheduler;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class EssenciumSchedulerIntegrationTest {

  private final EssenciumScheduler essenciumScheduler;

  private final SessionTokenRepository sessionTokenRepository;
  private final ApiTokenRepository apiTokenRepository;

  @Container
  static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17");

  @Autowired
  public EssenciumSchedulerIntegrationTest(
      EssenciumScheduler essenciumScheduler,
      SessionTokenRepository sessionTokenRepository,
      ApiTokenRepository apiTokenRepository) {
    this.essenciumScheduler = essenciumScheduler;
    this.sessionTokenRepository = sessionTokenRepository;
    this.apiTokenRepository = apiTokenRepository;
  }

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
  }

  @AfterEach
  void tearDown() {
    sessionTokenRepository.deleteAll();
    apiTokenRepository.deleteAll();
  }

  @Test
  void sessionTokenCleanup() {
    SessionToken sessionTokenToKeep =
        createTestSessionToken(
            "testuser",
            Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
            Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
            SessionTokenType.REFRESH,
            null);
    createTestSessionAccessToken(sessionTokenToKeep);

    SessionToken sessionTokenToDelete =
        createTestSessionToken(
            "testuser",
            Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
            Date.from(Instant.now().minus(5, ChronoUnit.DAYS)),
            SessionTokenType.REFRESH,
            null);
    createTestSessionAccessToken(sessionTokenToDelete);

    assertEquals(4, sessionTokenRepository.count());

    essenciumScheduler.sessionTokenCleanup();

    List<SessionToken> tokenRepositoryAll = sessionTokenRepository.findAll();
    assertEquals(2, tokenRepositoryAll.size());
  }

  @Test
  void apiTokenExpirationCheck() {
    createTestApiToken();

    assertEquals(3, apiTokenRepository.count());
    assertEquals(3, sessionTokenRepository.count());

    essenciumScheduler.apiTokenExpirationCheck();

    List<ApiToken> apiTokenRepositoryAll = apiTokenRepository.findAll();
    assertEquals(3, apiTokenRepositoryAll.size());

    List<SessionToken> sessionTokenRepositoryAll = sessionTokenRepository.findAll();
    assertEquals(1, sessionTokenRepositoryAll.size());
  }

  @Test
  void deleteOldApiTokens() {
    createTestApiToken();

    assertEquals(3, apiTokenRepository.count());
    assertEquals(3, sessionTokenRepository.count());

    // First, expire the tokens so that they are marked as expired and the old ones can be deleted
    // Invalid SessionToken will be deleted by the cleanup
    essenciumScheduler.apiTokenExpirationCheck();
    // Delete "very invalid" token which is already expired for more than a month
    essenciumScheduler.deleteOldApiTokens();

    List<ApiToken> apiTokenRepositoryAll = apiTokenRepository.findAll();
    assertEquals(2, apiTokenRepositoryAll.size());

    List<SessionToken> sessionTokenRepositoryAll = sessionTokenRepository.findAll();
    assertEquals(1, sessionTokenRepositoryAll.size());
  }

  private void createTestApiToken() {
    ApiToken validApiToken =
        apiTokenRepository.save(
            ApiToken.builder()
                .linkedUser("testuser")
                .validUntil(LocalDate.now().plusDays(1))
                .description("Valid API Token")
                .status(ApiTokenStatus.ACTIVE)
                .token("test-token")
                .build());
    createTestSessionTokenFrom(validApiToken);
    ApiToken invalidApiToken =
        apiTokenRepository.save(
            ApiToken.builder()
                .linkedUser("testuser")
                .validUntil(LocalDate.now().minusDays(1))
                .description("Invalid API Token")
                .status(ApiTokenStatus.ACTIVE)
                .token("test_token")
                .build());
    createTestSessionTokenFrom(invalidApiToken);
    ApiToken veryInvalidApiToken =
        apiTokenRepository.save(
            ApiToken.builder()
                .linkedUser("testuser")
                .validUntil(LocalDate.now().minusMonths(2))
                .description("Very Invalid API Token")
                .status(ApiTokenStatus.ACTIVE)
                .token("invalid_test_token")
                .build());
    createTestSessionTokenFrom(veryInvalidApiToken);
  }

  private void createTestSessionTokenFrom(ApiToken apiToken) {
    createTestSessionToken(
        apiToken.getUsername(),
        Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
        Date.from(
            apiToken.getValidUntil().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
        SessionTokenType.API,
        null);
  }

  private SessionToken createTestSessionToken(
      String username,
      Date issuedAt,
      Date expiration,
      SessionTokenType type,
      SessionToken parentToken) {
    return sessionTokenRepository.save(
        SessionToken.builder()
            .username(username)
            .key(Jwts.SIG.HS512.key().build())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .type(type)
            .parentToken(parentToken)
            .build());
  }

  private void createTestSessionAccessToken(SessionToken sessionToken) {
    createTestSessionToken(
        sessionToken.getUsername(),
        sessionToken.getIssuedAt(),
        sessionToken.getExpiration(),
        SessionTokenType.ACCESS,
        sessionToken);
  }
}
