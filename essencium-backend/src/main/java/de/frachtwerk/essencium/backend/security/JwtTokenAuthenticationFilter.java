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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.model.dto.RightGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.RoleGrantedAuthority;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Filter to extract a JWT Bearer token from the request's Authorization header and verify it */
public class JwtTokenAuthenticationFilter<ID extends Serializable>
    extends AbstractAuthenticationProcessingFilter {

  public static final String TOKEN_QUERY_PARAM = "t";
  private static final Pattern headerParamRegex =
      Pattern.compile("^Bearer ([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]*)$");

  @Autowired private JwtTokenService jwtTokenService;

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenAuthenticationFilter.class);

  public JwtTokenAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    super(requiresAuthenticationRequestMatcher);
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    LOGGER.debug(
        "attempting to extract jwt bearer token from authorization header or query string");

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

    return getAuthentication(token);
  }

  public Authentication getAuthentication(Object token) {
    try {
      if (!(token instanceof String)) {
        throw new BadCredentialsException("invalid token type");
      }
      Claims claims = jwtTokenService.verifyToken((String) token);

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

  public static String extractBearerToken(String param)
      throws AuthenticationCredentialsNotFoundException {
    Matcher m = headerParamRegex.matcher(param);
    if (!m.find() || m.groupCount() != 1) {
      throw new AuthenticationCredentialsNotFoundException("missing bearer token parameter");
    }
    return m.group(1).trim();
  }
}
