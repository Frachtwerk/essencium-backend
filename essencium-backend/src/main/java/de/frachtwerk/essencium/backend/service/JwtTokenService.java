/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.configuration.properties.JwtConfigProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.security.SessionTokenKeyLocator;
import io.jsonwebtoken.*;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import lombok.Setter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtTokenService implements Clock {

  // Claims: https://www.iana.org/assignments/jwt/jwt.xhtml#claims
  private final SessionTokenRepository sessionTokenRepository;
  private final SessionTokenKeyLocator sessionTokenKeyLocator;

  public static final String CLAIM_UID = "uid";
  public static final String CLAIM_NONCE = "nonce";
  public static final String CLAIM_FIRST_NAME = "given_name";
  public static final String CLAIM_LAST_NAME = "family_name";

  private final JwtConfigProperties jwtConfigProperties;

  @Setter
  private AbstractUserService<
          ? extends AbstractBaseUser<?>, ? extends Serializable, ? extends UserDto<?>>
      userService;

  private final UserMailService userMailService;

  public JwtTokenService(
      SessionTokenRepository sessionTokenRepository,
      SessionTokenKeyLocator sessionTokenKeyLocator,
      JwtConfigProperties jwtConfigProperties,
      UserMailService userMailService) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.sessionTokenKeyLocator = sessionTokenKeyLocator;
    this.jwtConfigProperties = jwtConfigProperties;
    this.userMailService = userMailService;
  }

  public String login(
      AbstractBaseUser<? extends Serializable> principal, @Nullable String userAgent) {
    return createToken(principal, SessionTokenType.REFRESH, userAgent, null);
  }

  public String createToken(
      AbstractBaseUser<? extends Serializable> user,
      SessionTokenType sessionTokenType,
      @Nullable String userAgent,
      @Nullable String bearerToken) {
    SessionToken requestingToken = null;
    if (Objects.nonNull(bearerToken)) {
      requestingToken = getRequestingToken(bearerToken);
    }

    SessionToken sessionToken =
        switch (sessionTokenType) {
          case ACCESS -> createToken(
              user,
              SessionTokenType.ACCESS,
              jwtConfigProperties.getAccessTokenExpiration(),
              userAgent,
              requestingToken);
          case REFRESH -> createToken(
              user,
              SessionTokenType.REFRESH,
              jwtConfigProperties.getRefreshTokenExpiration(),
              userAgent,
              null);
        };

    if (sessionTokenType == SessionTokenType.REFRESH) {
      TokenRepresentation tokenRepresentation =
          TokenRepresentation.builder()
              .id(sessionToken.getId())
              .type(sessionToken.getType())
              .issuedAt(sessionToken.getIssuedAt())
              .expiration(sessionToken.getExpiration())
              .userAgent(Objects.requireNonNullElse(sessionToken.getUserAgent(), ""))
              .build();
      userMailService.sendLoginMail(user.getEmail(), tokenRepresentation, user.getLocale());
    }

    return Jwts.builder()
        .header()
        .keyId(sessionToken.getId().toString())
        .type(sessionTokenType.name())
        .and()
        .subject(user.getUsername())
        .issuedAt(sessionToken.getIssuedAt())
        .expiration(sessionToken.getExpiration())
        .issuer(jwtConfigProperties.getIssuer())
        .claim(CLAIM_NONCE, user.getNonce())
        .claim(CLAIM_FIRST_NAME, user.getFirstName())
        .claim(CLAIM_LAST_NAME, user.getLastName())
        .claim(CLAIM_UID, user.getId())
        .signWith(sessionToken.getKey())
        .compact();
  }

  private SessionToken getRequestingToken(String bearerToken) {
    Jwt<?, ?> parse =
        Jwts.parser()
            .keyLocator(sessionTokenKeyLocator)
            .requireIssuer(jwtConfigProperties.getIssuer())
            .clock(this)
            .build()
            .parse(bearerToken);
    String kid = (String) parse.getHeader().get("kid");
    UUID id = UUID.fromString(kid);
    return sessionTokenRepository.getReferenceById(id);
  }

  private SessionToken createToken(
      AbstractBaseUser<? extends Serializable> user,
      SessionTokenType sessionTokenType,
      long accessTokenExpiration,
      String userAgent,
      @Nullable SessionToken refreshToken) {
    if (sessionTokenType == SessionTokenType.ACCESS && refreshToken != null) {
      sessionTokenRepository.deleteAll(sessionTokenRepository.findAllByParentToken(refreshToken));
    }
    Date now = now();
    Date expiration = Date.from(now.toInstant().plusSeconds(accessTokenExpiration));
    SecretKey key = Jwts.SIG.HS512.key().build();
    return sessionTokenRepository.save(
        SessionToken.builder()
            .key(key)
            .username(user.getUsername())
            .type(sessionTokenType)
            .issuedAt(now)
            .expiration(expiration)
            .userAgent(userAgent)
            .parentToken(refreshToken)
            .build());
  }

  public Claims verifyToken(String token) {
    return Jwts.parser()
        .keyLocator(sessionTokenKeyLocator)
        .requireIssuer(jwtConfigProperties.getIssuer())
        .clock(this)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public String renew(String bearerToken, String userAgent) {
    SessionToken sessionToken = getRequestingToken(bearerToken);
    AbstractBaseUser<? extends Serializable> user =
        userService.loadUserByUsername(sessionToken.getUsername());
    if (Objects.equals(sessionToken.getType(), SessionTokenType.REFRESH)) {
      return createToken(user, SessionTokenType.ACCESS, userAgent, bearerToken);
    } else {
      throw new IllegalArgumentException("Session token is not a refresh token");
    }
  }

  public List<SessionToken> getTokens(String username) {
    return sessionTokenRepository.findAllByUsernameAndType(username, SessionTokenType.REFRESH);
  }

  public void deleteToken(String username, UUID id) {
    SessionToken sessionToken = sessionTokenRepository.getReferenceById(id);
    if (Objects.equals(sessionToken.getUsername(), username)) {
      // delete all ACCESS_TOKENs that belong to this REFRESH_TOKEN
      sessionTokenRepository.deleteAll(sessionTokenRepository.findAllByParentToken(sessionToken));
      // delete REFRESH_TOKEN
      sessionTokenRepository.delete(sessionToken);
    } else {
      throw new IllegalArgumentException("Session token does not belong to user");
    }
  }

  @Transactional
  @Scheduled(fixedRateString = "${app.auth.jwt.cleanup-interval}", timeUnit = TimeUnit.SECONDS)
  public void cleanup() {
    sessionTokenRepository.deleteAllByExpirationBefore(now());
  }

  @Override
  public Date now() {
    return new Date();
  }
}
