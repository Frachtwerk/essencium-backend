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
}
