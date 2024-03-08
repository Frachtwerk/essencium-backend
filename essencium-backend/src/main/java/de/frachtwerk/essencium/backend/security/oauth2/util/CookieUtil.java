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

package de.frachtwerk.essencium.backend.security.oauth2.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

public class CookieUtil {

  public static final String OAUTH2_REQUEST_COOKIE_NAME = "oauth2_auth_request";

  public static final String REDIRECT_URI_PARAM = "redirect_uri";
  private static final int COOKIE_EXPIRE_SECONDS = 180;

  private CookieUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static void addCookie(HttpServletResponse response, String name, String value) {

    Cookie cookie = new Cookie(name, value);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
    response.addCookie(cookie);
  }

  public static Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
    Optional<Cookie[]> cookiesOptional =
        Optional.ofNullable(request.getCookies()).filter(cookies -> cookies.length > 0);
    if (cookiesOptional.isPresent()) {

      for (Cookie cookie : cookiesOptional.get()) {
        if (cookie.getName().equals(cookieName)) {
          return Optional.of(cookie.getValue());
        }
      }
    }
    return Optional.empty();
  }

  public static void deleteCookie(
      HttpServletRequest request, HttpServletResponse response, String cookieName) {
    Optional<Cookie[]> cookiesOptional =
        Optional.ofNullable(request.getCookies()).filter(cookies -> cookies.length > 0);
    if (cookiesOptional.isPresent()) {
      for (Cookie cookie : cookiesOptional.get()) {
        if (cookie.getName().equals(cookieName)) {
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          response.addCookie(cookie);
        }
      }
    }
  }
}
