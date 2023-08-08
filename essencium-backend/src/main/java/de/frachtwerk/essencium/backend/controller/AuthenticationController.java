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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.LoginRequest;
import de.frachtwerk.essencium.backend.model.dto.TokenResponse;
import de.frachtwerk.essencium.backend.security.event.CustomAuthenticationSuccessEvent;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class AuthenticationController {

  private final JwtTokenService jwtTokenService;
  private final AuthenticationManager authenticationManager;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public AuthenticationController(
      JwtTokenService jwtTokenService,
      AuthenticationManager authenticationManager,
      ApplicationEventPublisher applicationEventPublisher) {
    this.jwtTokenService = jwtTokenService;
    this.authenticationManager = authenticationManager;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @PostMapping("/token")
  @Operation(description = "Log in to request a new JWT token")
  public TokenResponse postLogin(@RequestBody @Validated LoginRequest login) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(login.username(), login.password()));
      applicationEventPublisher.publishEvent(
          new CustomAuthenticationSuccessEvent(
              authentication,
              String.format("Login successful for user %s", authentication.getName())));
      return new TokenResponse(
          jwtTokenService.createToken((AbstractBaseUser) authentication.getPrincipal()));
    } catch (AuthenticationException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
    }
  }

  @PostMapping("/renew")
  @Operation(description = "Request a new JWT token, given a valid one")
  public TokenResponse postRenew(
      @Parameter(hidden = true) @AuthenticationPrincipal AbstractBaseUser user) {
    try {
      return new TokenResponse(jwtTokenService.createToken(user));
    } catch (AuthenticationException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
    }
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS);
  }
}
