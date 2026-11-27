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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Access rules of the endpoints, shared by {@link WebSecurityConfig} (request matchers) and {@link
 * PublicEndpointsOpenApiCustomizer} (documented security requirements).
 *
 * <p>The rules start with the Essencium defaults (see {@link Builder#withDefaults()}) and can be
 * extended or overridden by registering {@link EndpointAccessCustomizer} beans.
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

  /**
   * A path pattern (Spring {@link PathPattern} syntax, relative to the servlet path), optionally
   * restricted to an HTTP method.
   *
   * @param method HTTP method, {@code null} for all methods
   * @param pattern path pattern, e.g. {@code /v1/translations/**}
   */
  public record Rule(@Nullable HttpMethod method, String pattern) {

    public static Rule of(String pattern) {
      return new Rule(null, pattern);
    }

    public static Rule of(HttpMethod method, String pattern) {
      return new Rule(method, pattern);
    }
  }

  /** a rule with its pattern parsed once, as a whole and per segment */
  private record CompiledRule(Rule rule, PathPattern pathPattern, List<Segment> segments) {

    static List<CompiledRule> compile(List<Rule> rules) {
      return rules.stream()
          .map(rule ->
              new CompiledRule(
                  rule,
                  PathPatternParser.defaultInstance.parse(rule.pattern()),
                  Segment.split(rule.pattern())))
          .toList();
    }

    boolean matches(@Nullable HttpMethod method, PathContainer path) {
      return methodMatches(method) && pathPattern.matches(path);
    }

    /**
     * Matches a URI template like {@code /v2/orders/{id}} segment by segment.
     *
     * @param possibly {@code true}: the rule matches at least one value of the template variables;
     *     {@code false}: the rule matches all values
     */
    boolean matchesTemplate(@Nullable HttpMethod method, List<String> template, boolean possibly) {
      return methodMatches(method) && matchesTemplate(0, template, 0, possibly);
    }

    private boolean matchesTemplate(
        int segmentIndex, List<String> template, int templateIndex, boolean possibly) {
      if (segmentIndex == segments.size()) {
        return templateIndex == template.size();
      }
      Segment segment = segments.get(segmentIndex);
      if (segment.kind() == SegmentKind.MULTI) {
        for (int i = templateIndex; i <= template.size(); i++) {
          if (matchesTemplate(segmentIndex + 1, template, i, possibly)) {
            return true;
          }
        }
        return false;
      }
      return templateIndex < template.size()
          && segment.matches(template.get(templateIndex), possibly)
          && matchesTemplate(segmentIndex + 1, template, templateIndex + 1, possibly);
    }

    private boolean methodMatches(@Nullable HttpMethod method) {
      return rule.method() == null || rule.method().equals(method);
    }
  }

  private enum SegmentKind {
    /** {@code **} or {@code {*name}}: any number of segments */
    MULTI,
    /** {@code *} or {@code {name}}: any single segment */
    ANY,
    /** literal, partial wildcard or variable with regex */
    OTHER
  }

  private record Segment(SegmentKind kind, @Nullable PathPattern pathPattern) {

    private static final Pattern ANY_VARIABLE = Pattern.compile("\\{[^:*}]+}");

    static List<Segment> split(String pattern) {
      return segmentsOf(pattern).stream().map(Segment::of).toList();
    }

    private static Segment of(String segment) {
      if (segment.equals("**") || segment.startsWith("{*")) {
        return new Segment(SegmentKind.MULTI, null);
      }
      if (segment.equals("*") || ANY_VARIABLE.matcher(segment).matches()) {
        return new Segment(SegmentKind.ANY, null);
      }
      return new Segment(SegmentKind.OTHER, PathPatternParser.defaultInstance.parse("/" + segment));
    }

    boolean matches(String templateSegment, boolean possibly) {
      if (kind == SegmentKind.ANY) {
        return true;
      }
      if (isTemplated(templateSegment)) {
        // the values of a template variable are unknown
        return possibly;
      }
      return pathPattern.matches(PathContainer.parsePath("/" + templateSegment));
    }
  }

  private final List<Rule> protectedRules;
  private final List<Rule> publicRules;
  private final List<Rule> optionalAuthRules;
  private final List<Rule> controllerAuthRules;

  private final List<CompiledRule> compiledProtectedRules;
  private final List<CompiledRule> compiledPublicRules;
  private final List<CompiledRule> compiledOptionalAuthRules;
  private final List<CompiledRule> compiledControllerAuthRules;

  private final RequestMatcher protectedUrls;
  private final RequestMatcher publicUrls;

  private EssenciumEndpointAccess(Builder builder) {
    this.protectedRules = List.copyOf(builder.protectedRules);
    this.publicRules = List.copyOf(builder.publicRules);
    this.optionalAuthRules = List.copyOf(builder.optionalAuthRules);
    this.controllerAuthRules = List.copyOf(builder.controllerAuthRules);

    this.compiledProtectedRules = CompiledRule.compile(protectedRules);
    this.compiledPublicRules = CompiledRule.compile(publicRules);
    this.compiledOptionalAuthRules = CompiledRule.compile(optionalAuthRules);
    this.compiledControllerAuthRules = CompiledRule.compile(controllerAuthRules);

    this.protectedUrls = anyOf(protectedRules);
    this.publicUrls =
        new OrRequestMatcher(
            new NegatedRequestMatcher(protectedUrls),
            anyOf(publicRules),
            // run the full filter chain to provide the user object if an auth header is present,
            // but otherwise let the request pass anyway
            new AndRequestMatcher(
                anyOf(optionalAuthRules),
                new NegatedRequestMatcher(
                    new RequestHeaderRequestMatcher(HttpHeaders.AUTHORIZATION))));
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return the Essencium default rules without any customization
   */
  public static EssenciumEndpointAccess defaults() {
    return builder().withDefaults().build();
  }

  /** requests requiring authentication, unless matched by {@link #publicUrls()} */
  public RequestMatcher protectedUrls() {
    return protectedUrls;
  }

  /** requests excluded from Spring Security */
  public RequestMatcher publicUrls() {
    return publicUrls;
  }

  public List<Rule> protectedRules() {
    return protectedRules;
  }

  public List<Rule> publicRules() {
    return publicRules;
  }

  public List<Rule> optionalAuthRules() {
    return optionalAuthRules;
  }

  public List<Rule> controllerAuthRules() {
    return controllerAuthRules;
  }

  /**
   * Determines the documented access of a request: how the security configuration treats it, except
   * for {@link #controllerAuthRules()}, which are {@link Access#PROTECTED} although the filter
   * chain lets them pass.
   *
   * <p>For a URI template like {@code /v2/orders/{id}} the access is determined conservatively: it
   * is only {@link Access#PUBLIC} or {@link Access#OPTIONAL} if this applies to all values of the
   * template variables.
   *
   * @param method HTTP method of the request, {@code null} matches only method independent rules
   * @param path request path or URI template relative to the servlet path, e.g. {@code
   *     /v1/users/{id}}
   */
  public Access of(@Nullable HttpMethod method, String path) {
    List<String> segments = segmentsOf(path);
    if (segments.stream().anyMatch(EssenciumEndpointAccess::isTemplated)) {
      return ofTemplate(method, segments);
    }
    PathContainer pathContainer = PathContainer.parsePath(path);
    if (matchesAny(compiledControllerAuthRules, method, pathContainer)) {
      return Access.PROTECTED;
    }
    if (!matchesAny(compiledProtectedRules, method, pathContainer)
        || matchesAny(compiledPublicRules, method, pathContainer)) {
      return Access.PUBLIC;
    }
    if (matchesAny(compiledOptionalAuthRules, method, pathContainer)) {
      return Access.OPTIONAL;
    }
    return Access.PROTECTED;
  }

  private Access ofTemplate(@Nullable HttpMethod method, List<String> template) {
    if (matchesAny(compiledControllerAuthRules, method, template, true)) {
      return Access.PROTECTED;
    }
    if (!matchesAny(compiledProtectedRules, method, template, true)
        || matchesAny(compiledPublicRules, method, template, false)) {
      return Access.PUBLIC;
    }
    if (matchesAny(compiledOptionalAuthRules, method, template, false)) {
      return Access.OPTIONAL;
    }
    return Access.PROTECTED;
  }

  private static boolean matchesAny(
      List<CompiledRule> rules,
      @Nullable HttpMethod method,
      List<String> template,
      boolean possibly) {
    return rules.stream().anyMatch(rule -> rule.matchesTemplate(method, template, possibly));
  }

  private static List<String> segmentsOf(String path) {
    return List.of((path.startsWith("/") ? path.substring(1) : path).split("/", -1));
  }

  private static boolean isTemplated(String segment) {
    return segment.contains("{");
  }

  private static boolean matchesAny(
      List<CompiledRule> rules, @Nullable HttpMethod method, PathContainer path) {
    return rules.stream().anyMatch(rule -> rule.matches(method, path));
  }

  private static RequestMatcher anyOf(List<Rule> rules) {
    if (rules.isEmpty()) {
      return request -> false;
    }
    return new OrRequestMatcher(
        rules.stream()
            .map(rule ->
                PathPatternRequestMatcher.withDefaults().matcher(rule.method(), rule.pattern()))
            .map(RequestMatcher.class::cast)
            .toList());
  }

  /**
   * Collects the rules. The rule lists are mutable, so {@link EndpointAccessCustomizer}s can remove
   * or replace default rules in addition to the convenience methods.
   */
  public static final class Builder {

    private final List<Rule> protectedRules = new ArrayList<>();
    private final List<Rule> publicRules = new ArrayList<>();
    private final List<Rule> optionalAuthRules = new ArrayList<>();
    private final List<Rule> controllerAuthRules = new ArrayList<>();

    private Builder() {}

    /**
     * Adds the Essencium defaults:
     *
     * <ul>
     *   <li>protected: {@code /v1/**}
     *   <li>public: {@code GET /v1/translations/**}, {@code /v1/reset-credentials/**}, {@code
     *       /v1/set-password/**}, {@code /v3/api-docs/**}, {@code /swagger-ui/**}
     *   <li>optional authentication: {@code /v1/contact/**}
     *   <li>token checked by the controller: {@code POST /auth/renew}, {@code POST /auth/logout}
     * </ul>
     */
    public Builder withDefaults() {
      protect("/v1/**");
      permit(HttpMethod.GET, "/v1/translations/**");
      permit("/v1/reset-credentials/**");
      permit("/v1/set-password/**");
      permit("/v3/api-docs/**");
      permit("/swagger-ui/**");
      optionalAuth("/v1/contact/**");
      tokenCheckedByController(HttpMethod.POST, "/auth/renew");
      tokenCheckedByController(HttpMethod.POST, "/auth/logout");
      return this;
    }

    /** requires authentication for matching requests, unless a public rule matches as well */
    public Builder protect(String pattern) {
      return protect(null, pattern);
    }

    public Builder protect(@Nullable HttpMethod method, String pattern) {
      protectedRules.add(new Rule(method, pattern));
      return this;
    }

    /**
     * excludes matching requests from Spring Security; requests outside all protected rules are
     * public anyway
     */
    public Builder permit(String pattern) {
      return permit(null, pattern);
    }

    public Builder permit(@Nullable HttpMethod method, String pattern) {
      publicRules.add(new Rule(method, pattern));
      return this;
    }

    /**
     * evaluates the authentication of matching protected requests only if an {@code Authorization}
     * header is present, otherwise lets them pass anonymously
     */
    public Builder optionalAuth(String pattern) {
      return optionalAuth(null, pattern);
    }

    public Builder optionalAuth(@Nullable HttpMethod method, String pattern) {
      optionalAuthRules.add(new Rule(method, pattern));
      return this;
    }

    /**
     * documents matching public requests as protected because the controller checks the bearer
     * token itself; does not change the request matchers
     */
    public Builder tokenCheckedByController(@Nullable HttpMethod method, String pattern) {
      controllerAuthRules.add(new Rule(method, pattern));
      return this;
    }

    public List<Rule> protectedRules() {
      return protectedRules;
    }

    public List<Rule> publicRules() {
      return publicRules;
    }

    public List<Rule> optionalAuthRules() {
      return optionalAuthRules;
    }

    public List<Rule> controllerAuthRules() {
      return controllerAuthRules;
    }

    public EssenciumEndpointAccess build() {
      return new EssenciumEndpointAccess(this);
    }
  }
}
