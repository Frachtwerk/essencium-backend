package de.frachtwerk.essencium.backend.model;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTokenTest {

  @Test
  void getLastUsed1() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
            .userAgent(userAgent)
            .accessTokens(List.of())
            .build();
    assertNull(sessionToken.getLastUsed());
  }

  @Test
  void getLastUsed2() {
    String userAgent = "Unit Test";
    LocalDateTime now = LocalDateTime.now();
    Date issuedAtAccessToken = Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC));
    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(now.minusMonths(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(now.plusMonths(1).toInstant(ZoneOffset.UTC)))
            .userAgent(userAgent)
            .accessTokens(
                List.of(
                    SessionToken.builder()
                        .id(UUID.randomUUID())
                        .key(Jwts.SIG.HS512.key().build())
                        .username("test@example.com")
                        .type(SessionTokenType.ACCESS)
                        .issuedAt(issuedAtAccessToken)
                        .expiration(Date.from(now.plusDays(1).toInstant(ZoneOffset.UTC)))
                        .userAgent(userAgent)
                        .accessTokens(List.of())
                        .build()))
            .build();

    assertEquals(sessionToken.getLastUsed(), issuedAtAccessToken);
  }
}
