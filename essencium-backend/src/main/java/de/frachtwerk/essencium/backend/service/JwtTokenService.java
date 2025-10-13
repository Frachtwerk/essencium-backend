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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.configuration.properties.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import de.frachtwerk.essencium.backend.security.JwtTokenAuthenticationFilter;
import de.frachtwerk.essencium.backend.security.SessionTokenKeyLocator;
import io.jsonwebtoken.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.sentry.spring.jakarta.tracing.SentryTransaction;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenService implements Clock {

  // Claims: https://www.iana.org/assignments/jwt/jwt.xhtml#claims
  private final SessionTokenRepository sessionTokenRepository;
  private final SessionTokenKeyLocator sessionTokenKeyLocator;

  public static final String CLAIM_UID = "uid";
  public static final String CLAIM_FIRST_NAME = "given_name";
  public static final String CLAIM_LAST_NAME = "family_name";
  public static final String CLAIM_ROLES = "roles";
  public static final String CLAIM_RIGHTS = "rights";

  public static final String CLAIM_LOCALE = "locale";
  private final AppJwtProperties appJwtProperties;

  @Setter
  private AbstractUserService<
          ? extends AbstractBaseUser<?>,
          ? extends EssenciumUserDetails<?>,
          ? extends Serializable,
          ? extends BaseUserDto<?>>
      userService;

  private final UserMailService userMailService;

  public JwtTokenService(
      SessionTokenRepository sessionTokenRepository,
      SessionTokenKeyLocator sessionTokenKeyLocator,
      AppJwtProperties appJwtProperties,
      UserMailService userMailService) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.sessionTokenKeyLocator = sessionTokenKeyLocator;
    this.appJwtProperties = appJwtProperties;
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
          case ACCESS ->
              createToken(
                  user,
                  SessionTokenType.ACCESS,
                  appJwtProperties.getAccessTokenExpiration(),
                  userAgent,
                  requestingToken);
          case REFRESH ->
              createToken(
                  user,
                  SessionTokenType.REFRESH,
                  appJwtProperties.getRefreshTokenExpiration(),
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

    JwtBuilder jwtsBuilder =
        Jwts.builder()
            .header()
            .keyId(sessionToken.getId().toString())
            .type(sessionTokenType.name())
            .and()
            .subject(user.getUsername())
            .issuedAt(sessionToken.getIssuedAt())
            .expiration(sessionToken.getExpiration())
            .issuer(appJwtProperties.getIssuer())
            .claim(CLAIM_FIRST_NAME, user.getFirstName())
            .claim(CLAIM_LAST_NAME, user.getLastName())
            .claim(CLAIM_UID, user.getId())
            .claim(CLAIM_ROLES, user.getRoles().stream().map(Role::getAuthority).toList())
            .claim(
                CLAIM_RIGHTS,
                user.getRoles().stream()
                    .flatMap(role -> role.getRights().stream().map(Right::getAuthority))
                    .collect(Collectors.toSet()))
            .claim(CLAIM_LOCALE, user.getLocale());
    for (Map.Entry<String, Object> entry : user.getAdditionalClaims().entrySet()) {
      jwtsBuilder.claim(entry.getKey(), entry.getValue());
    }
    return jwtsBuilder.signWith(sessionToken.getKey()).compact();
  }

  public SessionToken getRequestingToken(String bearerToken) {
    Jwt<?, ?> parse =
        Jwts.parser()
            .keyLocator(sessionTokenKeyLocator)
            .requireIssuer(appJwtProperties.getIssuer())
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
      // invalidate all ACCESS_TOKENs that belong to this REFRESH_TOKEN
      sessionTokenRepository.findAllByParentToken(refreshToken).stream()
          .filter(sessionToken -> sessionToken.getExpiration().after(now()))
          .forEach(
              sessionToken -> {
                sessionToken.setExpiration(now());
                sessionTokenRepository.save(sessionToken);
              });
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
    try {
      return Jwts.parser()
          .keyLocator(sessionTokenKeyLocator)
          .requireIssuer(appJwtProperties.getIssuer())
          .clock(this)
          .build()
          .parseSignedClaims(token)
          .getPayload();

    } catch (ExpiredJwtException e) {
      throw new SessionAuthenticationException("Session expired");
    }
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

  @SentryTransaction(operation = "JwtTokenService.cleanup")
  @Transactional
  @Scheduled(fixedRateString = "${app.auth.jwt.cleanup-interval}", timeUnit = TimeUnit.SECONDS)
  public void cleanup() {
    sessionTokenRepository.deleteAllByExpirationBefore(
        Date.from(
            LocalDateTime.now()
                .minusSeconds(appJwtProperties.getMaxSessionExpirationTime())
                .toInstant(ZoneOffset.UTC)));
  }

  @Override
  public Date now() {
    return new Date();
  }

  public boolean isAccessTokenValid(String refresh, String access) {
    SessionToken refreshToken = getRequestingToken(refresh);
    SessionToken accessToken = getRequestingToken(access);
    try {
      return Objects.equals(refreshToken, accessToken.getParentToken());
    } catch (NullPointerException e) {
      return false;
    }
  }

  /**
   * Logs out the user by deleting the session token and redirecting to the specified URI. If the
   * user is authenticated via OAuth2, it redirects to the logout URI of the OAuth2 provider.
   *
   * <p>This method is the preferred way to log out users, as it handles both local and OAuth2 and
   * redirects to a specified URI.
   *
   * @param authorizationHeader the authorization header containing the Bearer token
   * @param redirectUri the URI to redirect to after logout, can be null (if null, no redirect will
   *     occur)
   * @param oAuth2ClientRegistrationProperties the OAuth2 client registration properties
   * @param response the HTTP response to send the redirect
   */
  public void logout(
      @NotNull String authorizationHeader,
      @Nullable URI redirectUri,
      @NotNull OAuth2ClientRegistrationProperties oAuth2ClientRegistrationProperties,
      @NotNull HttpServletResponse response) {
    String token =
        Optional.ofNullable(authorizationHeader)
            .map(JwtTokenAuthenticationFilter::extractBearerToken)
            .filter(s -> !s.isEmpty())
            .orElseThrow(
                () ->
                    new AuthenticationCredentialsNotFoundException(
                        "missing authorization header parameter"));
    SessionToken requestingToken = getRequestingToken(token);

    AbstractBaseUser<? extends Serializable> user =
        userService.loadUserByUsername(requestingToken.getUsername());

    Optional.ofNullable(requestingToken.getParentToken())
        .ifPresentOrElse(
            parentToken -> deleteToken(requestingToken.getUsername(), parentToken.getId()),
            () -> deleteToken(requestingToken.getUsername(), requestingToken.getId()));

    String source = user.getSource();

    if (StringUtils.isBlank(source)
        || StringUtils.equalsIgnoreCase(source, AbstractBaseUser.USER_AUTH_SOURCE_LDAP)
        || StringUtils.equalsIgnoreCase(source, AbstractBaseUser.USER_AUTH_SOURCE_LOCAL)) {
      // If the user is not authenticated via OAuth2, redirect to the specified URI
      createRedirectOnLogout(redirectUri, response);
      return;
    }

    String provider =
        oAuth2ClientRegistrationProperties.getRegistration().get(source).getProvider();
    String logoutUri =
        oAuth2ClientRegistrationProperties.getProvider().get(provider).getLogoutUri();
    URI uri = Optional.ofNullable(logoutUri).map(URI::create).orElse(redirectUri);

    createRedirectOnLogout(uri, response);
  }

  private static void createRedirectOnLogout(URI redirectUri, HttpServletResponse response) {
    if (Objects.nonNull(redirectUri)) {
      try {
        response.sendRedirect(redirectUri.toString());
      } catch (IOException e) {
        log.error("Could not redirect to {}", redirectUri, e);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      }
    } else {
      log.warn("No redirect URI provided for logout, user will not be redirected after logout.");
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
  }

  public void deleteAllbyUsernameEqualsIgnoreCase(String username) {
    sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(username);
  }
}
