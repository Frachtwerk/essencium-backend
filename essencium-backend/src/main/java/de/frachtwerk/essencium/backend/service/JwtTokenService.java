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
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.security.SessionTokenKeyLocator;
import io.jsonwebtoken.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

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

  public JwtTokenService(
      SessionTokenRepository sessionTokenRepository,
      SessionTokenKeyLocator sessionTokenKeyLocator,
      JwtConfigProperties jwtConfigProperties) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.sessionTokenKeyLocator = sessionTokenKeyLocator;
    this.jwtConfigProperties = jwtConfigProperties;
  }

  public String createToken(AbstractBaseUser user, SessionTokenType sessionTokenType) {
    return switch (sessionTokenType) {
      case ACCESS -> createToken(
          user, SessionTokenType.ACCESS, jwtConfigProperties.getAccessTokenExpiration());
      case REFRESH -> createToken(
          user, SessionTokenType.REFRESH, jwtConfigProperties.getRefreshTokenExpiration());
      default -> throw new IllegalArgumentException(
          "Unknown session token type: " + sessionTokenType);
    };
  }

  private String createToken(
      AbstractBaseUser user, SessionTokenType sessionTokenType, long accessTokenExpiration) {
    Date now = now();
    Date expiration = Date.from(now.toInstant().plusSeconds(accessTokenExpiration));
    SecretKey key = Jwts.SIG.HS512.key().build();

    byte[] encoded = key.getEncoded();
    String encodedString = Base64.getEncoder().encodeToString(encoded);
    System.out.println("encodedString = " + encodedString);
    SessionToken sessionToken =
        sessionTokenRepository.save(
            SessionToken.builder()
                // .key(Base64.getEncoder().encodeToString(key.getEncoded()))
                .key(key)
                .username(user.getUsername())
                .type(sessionTokenType)
                .issuedAt(now)
                .expiration(expiration)
                .build());

    return Jwts.builder()
        .header()
        .keyId(sessionToken.getId().toString())
        .type(sessionTokenType.name())
        .and()
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(expiration)
        .issuer(jwtConfigProperties.getIssuer())
        .claim(CLAIM_NONCE, user.getNonce())
        .claim(CLAIM_FIRST_NAME, user.getFirstName())
        .claim(CLAIM_LAST_NAME, user.getLastName())
        .claim(CLAIM_UID, user.getId())
        .signWith(key)
        .compact();
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

  public String renew(AbstractBaseUser user, String bearerToken) {
    Jwt<?, ?> parse =
        Jwts.parser()
            .keyLocator(sessionTokenKeyLocator)
            .requireIssuer(jwtConfigProperties.getIssuer())
            .clock(this)
            .build()
            .parse(bearerToken);
    String kid = (String) parse.getHeader().get("kid");
    UUID id = UUID.fromString(kid);
    SessionToken sessionToken = sessionTokenRepository.getReferenceById(id);
    if (Objects.equals(sessionToken.getType(), SessionTokenType.REFRESH)) {
      return createToken(user, SessionTokenType.ACCESS);
    } else {
      throw new IllegalArgumentException("Session token is not a refresh token");
    }
  }

  @Override
  public Date now() {
    return new Date();
  }
}
