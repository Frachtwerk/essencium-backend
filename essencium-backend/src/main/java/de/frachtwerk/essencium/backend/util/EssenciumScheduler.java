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

package de.frachtwerk.essencium.backend.util;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.ApiTokenStatus;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import io.sentry.spring7.tracing.SentryTransaction;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EssenciumScheduler {
  private final SessionTokenRepository sessionTokenRepository;
  private final ApiTokenRepository apiTokenRepository;
  private final AppJwtProperties appJwtProperties;

  @SentryTransaction(operation = "EssenciumScheduler.sessionTokenCleanup")
  @Transactional
  @Scheduled(fixedRateString = "${app.auth.jwt.cleanup-interval}", timeUnit = TimeUnit.SECONDS)
  public void sessionTokenCleanup() {
    log.info("Starting session token cleanup task.");
    sessionTokenRepository.deleteAllByExpirationBefore(
        Date.from(
            LocalDateTime.now()
                .minusSeconds(appJwtProperties.getMaxSessionExpirationTime())
                .toInstant(ZoneOffset.UTC)));
  }

  @SentryTransaction(operation = "EssenciumScheduler.apiTokenExpirationCheck")
  @Transactional
  @Scheduled(cron = "0 0 0 * * *")
  public void apiTokenExpirationCheck() {
    log.info("Starting API token expiration check task.");
    LocalDate now = LocalDate.now();
    List<ApiToken> apiTokens =
        apiTokenRepository.findAllByStatusAndValidUntilBefore(ApiTokenStatus.ACTIVE, now);
    sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(
        apiTokens.stream()
            .map(ApiToken::getUsername)
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .toList());
    apiTokenRepository.setStatusByIds(
        ApiTokenStatus.EXPIRED, apiTokens.stream().map(ApiToken::getId).toList());
  }

  @Transactional
  @Scheduled(cron = "0 1 0 * * *")
  public void deleteOldApiTokens() {
    log.info("Starting old API token deletion task.");
    LocalDate cutoffDate = LocalDate.now().minusDays(30);
    apiTokenRepository.deleteAllByValidUntilBefore(cutoffDate);
  }
}
