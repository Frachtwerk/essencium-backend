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

package de.frachtwerk.essencium.backend.configuration;

import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Single source of truth for the access rules of the default endpoints. {@link WebSecurityConfig}
 * builds its request matchers from these rules, {@link PublicEndpointsOpenApiCustomizer} uses them
 * to document public endpoints without a security requirement.
 */
public final class EssenciumEndpointAccess {

  public enum Access {
    /** no authentication required */
    PUBLIC,
    /** authentication is evaluated if an {@code Authorization} header is present */
    OPTIONAL,
    /** authentication required */
    PROTECTED
  }

  record Rule(@Nullable HttpMethod method, String pattern) {
    boolean matches(@Nullable HttpMethod requestMethod, String path) {
      return (method == null || method.equals(requestMethod))
          && PathPatternParser.defaultInstance
              .parse(pattern)
              .matches(PathContainer.parsePath(path));
    }

    RequestMatcher toRequestMatcher() {
      return PathPatternRequestMatcher.withDefaults().matcher(method, pattern);
    }
  }

  static final Rule PROTECTED_RULE = new Rule(null, "/v1/**");

  /** exceptions within {@link #PROTECTED_RULE} that are accessible without authentication */
  static final List<Rule> PUBLIC_RULES =
      List.of(
          new Rule(HttpMethod.GET, "/v1/translations/**"),
          new Rule(null, "/v1/reset-credentials/**"),
          new Rule(null, "/v1/set-password/**"),
          new Rule(null, "/v3/api-docs/**"),
          new Rule(null, "/swagger-ui/**"));

  /**
   * exceptions within {@link #PROTECTED_RULE} that require authentication only if an {@code
   * Authorization} header is present, i.e. the full filter chain runs to provide the user object if
   * possible, but the request passes anyway
   */
  static final List<Rule> OPTIONAL_AUTH_RULES = List.of(new Rule(null, "/v1/contact/**"));

  static final RequestMatcher PROTECTED_URLS = PROTECTED_RULE.toRequestMatcher();

  static final RequestMatcher PUBLIC_URLS =
      new OrRequestMatcher(
          new NegatedRequestMatcher(PROTECTED_URLS),
          new OrRequestMatcher(PUBLIC_RULES.stream().map(Rule::toRequestMatcher).toList()),
          new AndRequestMatcher(
              new OrRequestMatcher(
                  OPTIONAL_AUTH_RULES.stream().map(Rule::toRequestMatcher).toList()),
              new NegatedRequestMatcher(
                  new RequestHeaderRequestMatcher(HttpHeaders.AUTHORIZATION))));

  private EssenciumEndpointAccess() {}

  /**
   * Determines how the default security configuration treats a request.
   *
   * @param method HTTP method of the request, {@code null} matches only method independent rules
   * @param path request path relative to the servlet path, e.g. {@code /v1/users/{id}}
   */
  public static Access of(@Nullable HttpMethod method, String path) {
    if (!PROTECTED_RULE.matches(method, path)
        || PUBLIC_RULES.stream().anyMatch(rule -> rule.matches(method, path))) {
      return Access.PUBLIC;
    }
    if (OPTIONAL_AUTH_RULES.stream().anyMatch(rule -> rule.matches(method, path))) {
      return Access.OPTIONAL;
    }
    return Access.PROTECTED;
  }
}
