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
