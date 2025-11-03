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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.Date;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EssenciumSchedulerTest {

  AppJwtProperties appConfigJwtProperties;

  @Mock SessionTokenRepository sessionTokenRepository;
  @Mock ApiTokenRepository apiTokenRepository;
  EssenciumScheduler essenciumScheduler;

  @BeforeEach
  void setUp() {
    appConfigJwtProperties = new AppJwtProperties();
    appConfigJwtProperties.setIssuer(RandomStringUtils.secure().nextAlphabetic(5, 10));
    appConfigJwtProperties.setAccessTokenExpiration(86400);
    appConfigJwtProperties.setRefreshTokenExpiration(2592000);
    appConfigJwtProperties.setMaxSessionExpirationTime(2592000);
    appConfigJwtProperties.setCleanupInterval(3600);
    essenciumScheduler =
        new EssenciumScheduler(sessionTokenRepository, apiTokenRepository, appConfigJwtProperties);
  }

  @Nested
  class SessionTokenCleanupTest {
    @Test
    void cleanupTest() {
      assertDoesNotThrow(() -> essenciumScheduler.sessionTokenCleanup());
      verify(sessionTokenRepository, times(1)).deleteAllByExpirationBefore(any(Date.class));
      verifyNoMoreInteractions(sessionTokenRepository);
    }
  }
}
