/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.security.oauth2;

import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ConfigProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

  private final OAuth2ConfigProperties oAuth2ConfigProperties;

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2FailureHandler.class);

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws ServletException, IOException {

    LOGGER.info("authentication failure requesting {}", request.getRequestURI());

    if (exception != null) {
      LOGGER.warn("error while using OAuth2 authentication: {}", exception.getLocalizedMessage());
    }

    if (Objects.nonNull(oAuth2ConfigProperties.getFailureRedirectUrl())) {
      final RedirectHandler redirectHandler =
          new RedirectHandler(oAuth2ConfigProperties.getFailureRedirectUrl());
      redirectHandler.onAuthenticationFailure(request, response, exception);
    }
  }

  class RedirectHandler extends SimpleUrlAuthenticationFailureHandler {
    public RedirectHandler(String failureRedirectUrl) {
      super(failureRedirectUrl);
    }
  }
}
