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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements Clock {

  // Claims: https://www.iana.org/assignments/jwt/jwt.xhtml#claims

  public static final String CLAIM_UID = "uid";
  public static final String CLAIM_NONCE = "nonce";
  public static final String CLAIM_FIRST_NAME = "given_name";
  public static final String CLAIM_LAST_NAME = "family_name";

  private final JwtConfigProperties jwtConfigProperties;
  private final SecretKeySpec secretKey;

  public JwtTokenService(JwtConfigProperties jwtConfigProperties) {
    this.jwtConfigProperties = jwtConfigProperties;
    // this.secretKey = BASE64.encode(jwtConfigProperties.getSecret());
    // this.secretKey = Encoders.BASE64.encode(jwtConfigProperties.getSecret().getBytes());
    this.secretKey =
        new SecretKeySpec(
            jwtConfigProperties.getSecret().getBytes(),
            0,
            jwtConfigProperties.getSecret().length(),
            SignatureAlgorithm.HS256.getJcaName());
  }

  public String createToken(AbstractBaseUser user) {
    return Jwts.builder()
        .setSubject(user.getUsername())
        .setIssuedAt(now())
        .setExpiration(
            Date.from(now().toInstant().plusSeconds(jwtConfigProperties.getExpiration())))
        .setIssuer(jwtConfigProperties.getIssuer())
        .claim(CLAIM_NONCE, user.getNonce())
        .claim(CLAIM_FIRST_NAME, user.getFirstName())
        .claim(CLAIM_LAST_NAME, user.getLastName())
        .claim(CLAIM_UID, user.getId())
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims verifyToken(String token) {
    return Jwts.parserBuilder()
        .requireIssuer(jwtConfigProperties.getIssuer())
        .setSigningKey(secretKey)
        .setClock(this)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  @Override
  public Date now() {
    return new Date();
  }
}
