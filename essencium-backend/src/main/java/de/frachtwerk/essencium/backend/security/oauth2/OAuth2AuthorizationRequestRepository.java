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

import de.frachtwerk.essencium.backend.security.oauth2.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class OAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME =
      OAuth2AuthorizationRequestRepository.class.getName() + ".AUTHORIZATION_REQUEST";
  private final String sessionAttributeName;

  public OAuth2AuthorizationRequestRepository() {
    this.sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;
  }

  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    Assert.notNull(request, "request cannot be null");
    String stateParameter = this.getStateParameter(request);
    if (stateParameter == null) {
      return null;
    } else {
      OAuth2AuthorizationRequest authorizationRequest = this.getAuthorizationRequest(request);
      return authorizationRequest != null && stateParameter.equals(authorizationRequest.getState())
          ? authorizationRequest
          : null;
    }
  }

  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {

    Assert.notNull(request, "request cannot be null");
    Assert.notNull(response, "response cannot be null");

    CookieUtil.addCookie(
        response,
        CookieUtil.OAUTH2_REQUEST_COOKIE_NAME,
        request.getParameter(CookieUtil.REDIRECT_URI_PARAM));

    if (authorizationRequest == null) {
      this.removeAuthorizationRequest(request, response);
    } else {
      String state = authorizationRequest.getState();
      Assert.hasText(state, "authorizationRequest.state cannot be empty");
      request.getSession().setAttribute(this.sessionAttributeName, authorizationRequest);
    }
  }

  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    Assert.notNull(response, "response cannot be null");
    OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
    if (authorizationRequest != null) {
      request.getSession().removeAttribute(this.sessionAttributeName);
    }

    return authorizationRequest;
  }

  private String getStateParameter(HttpServletRequest request) {
    return request.getParameter("state");
  }

  private OAuth2AuthorizationRequest getAuthorizationRequest(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null
        ? (OAuth2AuthorizationRequest) session.getAttribute(this.sessionAttributeName)
        : null;
  }
}
