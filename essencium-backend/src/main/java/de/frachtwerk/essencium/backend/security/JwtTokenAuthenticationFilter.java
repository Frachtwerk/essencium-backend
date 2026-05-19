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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.configuration.properties.security.AppTokenProperties;
import de.frachtwerk.essencium.backend.configuration.properties.security.IpOrCidrValidator;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.RightGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.RoleGrantedAuthority;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Filter to extract a JWT Bearer token from the request's Authorization header and verify it */
@Slf4j
public class JwtTokenAuthenticationFilter<ID extends Serializable>
    extends AbstractAuthenticationProcessingFilter {

  public static final String TOKEN_QUERY_PARAM = "t";
  private static final Pattern headerParamRegex =
      Pattern.compile("^Bearer ([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]*)$");

  @Autowired private JwtTokenService jwtTokenService;

  @Autowired private AppTokenProperties appTokenProperties;

  private final IpOrCidrValidator ipOrCidrValidator = new IpOrCidrValidator();

  public JwtTokenAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    super(requiresAuthenticationRequestMatcher);
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    log.debug("attempting to extract jwt bearer token from authorization header or query string");

    final String param =
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
            .orElse(request.getParameter(TOKEN_QUERY_PARAM));

    final String token =
        Optional.ofNullable(param)
            .map(JwtTokenAuthenticationFilter::extractBearerToken)
            .filter(s -> !s.isEmpty())
            .orElseThrow(
                () ->
                    new AuthenticationCredentialsNotFoundException(
                        "missing authorization header parameter"));

    return getAuthentication(token, request);
  }

  public Authentication getAuthentication(String token, HttpServletRequest request) {
    try {
      Jws<Claims> jws = jwtTokenService.verifyToken(token);
      String type = jws.getHeader().getType();
      Claims claims = jws.getPayload();

      if (Objects.nonNull(appTokenProperties.getAllowedIpAddresses())
          && !appTokenProperties.getAllowedIpAddresses().isEmpty()
          && SessionTokenType.valueOf(type).equals(SessionTokenType.API)) {
        verifyIpAddress(request);
      }

      if (Objects.nonNull(appTokenProperties.getPresharedSecrets())
          && !appTokenProperties.getPresharedSecrets().isEmpty()
          && SessionTokenType.valueOf(type).equals(SessionTokenType.API)) {
        verifyPresharedSecret(request);
      }

      @SuppressWarnings("unchecked")
      List<String> rolesRaw = claims.get(JwtTokenService.CLAIM_ROLES, List.class);
      List<RoleGrantedAuthority> roles =
          rolesRaw == null ? List.of() : rolesRaw.stream().map(RoleGrantedAuthority::new).toList();

      List<String> rightsRaw = claims.get(JwtTokenService.CLAIM_RIGHTS, List.class);
      List<RightGrantedAuthority> rights =
          rightsRaw == null
              ? List.of()
              : rightsRaw.stream().map(RightGrantedAuthority::new).toList();
      return new JwtAuthenticationToken<ID>(claims, roles, rights);
    } catch (SessionAuthenticationException e) {
      throw new BadCredentialsException(e.getMessage());
    } catch (SignatureException e) {
      throw new BadCredentialsException("invalid token");
    }
  }

  @Override
  protected void successfulAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      Authentication authResult)
      throws IOException, ServletException {
    super.successfulAuthentication(request, response, chain, authResult);
    chain.doFilter(request, response);
  }

  private void verifyIpAddress(HttpServletRequest request) {
    final String remoteAddr = request.getRemoteAddr();

    if (StringUtils.isBlank(remoteAddr)) {
      throw new NotAllowedException("Unable to determine remote IP address");
    }

    final String clientIp =
        resolveClientIp(remoteAddr.trim(), request.getHeader("X-Forwarded-For"));

    if (!ipOrCidrValidator.isValid(clientIp, null) || !isIpAllowed(clientIp)) {
      throw new NotAllowedException("IP address not allowed to use API tokens");
    }
  }

  /**
   * Determines the effective client IP from the request.
   *
   * <p>When {@code trustedProxies} is empty, {@code remoteAddr} is returned directly and {@code
   * X-Forwarded-For} is ignored, preventing header spoofing on direct connections.
   *
   * <p>When {@code trustedProxies} is configured, the full IP chain ({@code X-Forwarded-For}
   * entries followed by {@code remoteAddr}) is walked from right to left. Trusted-proxy addresses
   * are skipped; the first address that is not a trusted proxy is returned as the client IP. If
   * every address in the chain belongs to a trusted proxy the leftmost entry is returned as a
   * fallback (all hops are internal infrastructure).
   *
   * <p>{@code allowedIpAddresses} and {@code trustedProxies} must be disjoint: a proxy that appears
   * in {@code trustedProxies} is never treated as an allowed client.
   */
  String resolveClientIp(String remoteAddr, String xForwardedFor) {
    if (appTokenProperties.getTrustedProxies().isEmpty()) {
      return remoteAddr;
    }

    List<String> chain = new ArrayList<>();
    if (StringUtils.isNotBlank(xForwardedFor)) {
      Arrays.stream(xForwardedFor.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(chain::add);
    }
    chain.add(remoteAddr);

    for (int i = chain.size() - 1; i >= 0; i--) {
      String ip = chain.get(i);
      if (!isProxyTrusted(ip)) {
        return ip;
      }
    }

    // All hops are trusted proxies — fall back to leftmost (closest to the original client)
    return chain.getFirst();
  }

  private boolean isIpAllowed(String ip) {
    return appTokenProperties.getAllowedIpAddresses().stream()
        .anyMatch(allowed -> new IpAddressMatcher(allowed.trim()).matches(ip));
  }

  private boolean isProxyTrusted(String ip) {
    if (!ipOrCidrValidator.isValid(ip, null)) {
      return false;
    }
    return appTokenProperties.getTrustedProxies().stream()
        .anyMatch(trusted -> new IpAddressMatcher(trusted.trim()).matches(ip));
  }

  private void verifyPresharedSecret(HttpServletRequest request) {
    String header = request.getHeader(appTokenProperties.getPresharedSecretHeaderName());
    if (header == null || !appTokenProperties.getPresharedSecrets().contains(header)) {
      throw new NotAllowedException("Invalid preshared secret");
    }
  }

  public static String extractBearerToken(String param)
      throws AuthenticationCredentialsNotFoundException {
    Matcher m = headerParamRegex.matcher(param);
    if (!m.find() || m.groupCount() != 1) {
      throw new AuthenticationCredentialsNotFoundException("missing bearer token parameter");
    }
    return m.group(1).trim();
  }
}
