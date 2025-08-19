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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.configuration.properties.AppProperties;
import de.frachtwerk.essencium.backend.configuration.properties.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.model.dto.TokenResponse;
import de.frachtwerk.essencium.backend.security.JwtTokenAuthenticationFilter;
import de.frachtwerk.essencium.backend.security.event.CustomAuthenticationSuccessEvent;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.auth-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(name = "AuthenticationController", description = "Set of endpoints used for authentication")
@RequiredArgsConstructor
public class AuthenticationController {
  private final AppProperties appProperties;
  private final AppJwtProperties appJwtProperties;
  private final JwtTokenService jwtTokenService;
  private final JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
  private final AuthenticationManager authenticationManager;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final OAuth2ClientRegistrationProperties oAuth2ClientRegistrationProperties;
  private final AppOAuth2Properties appOAuth2Properties;

  public static String getBearerTokenHeader(HttpServletRequest request) {
    return request.getHeader(HttpHeaders.AUTHORIZATION);
  }

  @PostMapping("/token")
  @Operation(description = "Log in to request a new JWT token")
  public TokenResponse postLogin(
      @RequestBody @Validated LoginRequest login,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
      HttpServletResponse response) {
    try {
      // Authenticate using username and password
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(login.username(), login.password()));
      applicationEventPublisher.publishEvent(
          new CustomAuthenticationSuccessEvent(
              authentication,
              String.format("Login successful for user %s", authentication.getName())));

      // Get refresh token
      String refreshToken =
          jwtTokenService.login(
              (AbstractBaseUser<? extends Serializable>) authentication.getPrincipal(), userAgent);

      // Store refresh token as cookie limited to renew endpoint
      Cookie cookie = new Cookie("refreshToken", refreshToken);
      cookie.setHttpOnly(true);
      cookie.setPath("/auth/renew");
      cookie.setMaxAge(appJwtProperties.getRefreshTokenExpiration());
      cookie.setDomain(appProperties.getDomain());
      cookie.setSecure(true);

      response.addCookie(cookie);

      // create first access token and return it.
      return new TokenResponse(jwtTokenService.renew(refreshToken, userAgent));

    } catch (AuthenticationException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
    }
  }

  @PostMapping("/renew")
  @CrossOrigin(origins = "${app.url}", allowCredentials = "true")
  @Operation(description = "Request a new JWT access token, given a valid refresh token")
  public TokenResponse postRenew(
      @RequestHeader(value = HttpHeaders.USER_AGENT) String userAgent,
      @CookieValue(value = "refreshToken") String refreshToken,
      HttpServletRequest request) {
    // Check if refresh token is valid
    if (!jwtTokenAuthenticationFilter.getAuthentication(refreshToken).isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
    }

    // Check if session Token an access Token belong together
    String bearerToken = getBearerTokenHeader(request);
    if (Objects.nonNull(bearerToken)) {
      String accessToken =
          JwtTokenAuthenticationFilter.extractBearerToken(
              bearerToken); // bearerToken.replace("Bearer ", "");
      if (!jwtTokenService.isAccessTokenValid(refreshToken, accessToken)) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Refresh token and access token do not belong together");
      }
    } else {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No access token provided");
    }

    // Renew token
    return new TokenResponse(jwtTokenService.renew(refreshToken, userAgent));
  }

  @GetMapping("/oauth-registrations")
  public Map<String, Map<String, Object>> getRegistrations() {
    if (!appOAuth2Properties.isEnabled()
        || Objects.isNull(oAuth2ClientRegistrationProperties.getRegistration())) {
      return Map.of();
    }
    Map<String, Map<String, Object>> map =
        oAuth2ClientRegistrationProperties.getRegistration().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                      Map<String, Object> result = new HashMap<>();
                      result.put(
                          "name",
                          Objects.requireNonNullElse(
                              entry.getValue().getClientName(), entry.getKey()));
                      result.put("url", "/oauth2/authorization/" + entry.getKey());
                      result.put(
                          "imageUrl",
                          Objects.requireNonNullElse(entry.getValue().getImageUrl(), ""));
                      return result;
                    }));
    for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
      OAuth2ClientRegistrationProperties.ClientProvider clientProvider =
          oAuth2ClientRegistrationProperties.getProvider().get(entry.getKey());
      entry
          .getValue()
          .put(
              "allowSignup",
              Objects.requireNonNullElseGet(
                  clientProvider.getAllowSignup(), appOAuth2Properties::isAllowSignup));
      entry
          .getValue()
          .put(
              "updateRole",
              Objects.requireNonNullElseGet(
                  clientProvider.getUpdateRole(), appOAuth2Properties::isUpdateRole));
    }
    return map;
  }

  @PostMapping("/logout")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "redirectUrl",
      description = "URL to redirect to after logout",
      schema = @Schema(type = "string", format = "uri", example = "https://example.com/logout"))
  @Operation(summary = "Logout the currently logged-in user")
  public void logout(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
      HttpServletResponse response)
      throws IOException, URISyntaxException {
    if (StringUtils.isBlank(redirectUrl)) {
      redirectUrl = appProperties.getDefaultLogoutRedirectUrl();
    }
    // Create URI from redirect URL to validate it as a valid URI
    URI redirectUri = null;
    try {
      redirectUri = new URI(redirectUrl);
    } catch (URISyntaxException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid redirect URL: " + redirectUrl, e);
    }

    if (!isRedirectUrlAllowed(redirectUri)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Redirect URL is not allowed: " + redirectUri);
    }

    jwtTokenService.logout(
        authorizationHeader, redirectUri, oAuth2ClientRegistrationProperties, response);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS);
  }

  private boolean isRedirectUrlAllowed(URI redirectUrl) {
    return appProperties.getAllowedLogoutRedirectUrls().stream()
        .anyMatch(
            allowedUrl -> {
              String redirectUrlString = redirectUrl.toString();
              return allowedUrl.contains("*")
                  ? redirectUrlString.matches(allowedUrl.replace("*", ".*"))
                  : redirectUrlString.equals(allowedUrl);
            });
  }
}
