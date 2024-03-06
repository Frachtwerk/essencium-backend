package de.frachtwerk.essencium.backend.security.oauth2.util;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieUtilTest {

  @Test
  void testAddCookie() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    CookieUtil.addCookie(response, "test", "testValue");
    assertEquals("testValue", response.getCookie("test").getValue());
  }

  @Test
  void testGetCookieValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("test", "testValue"), new Cookie("test2", "testValue2"));

    CookieUtil.getCookieValue(request, "test").ifPresent(value -> assertEquals("testValue", value));
  }

  @Test
  void testGetCookieValueNoValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("test", "testValue"), new Cookie("test2", "testValue2"));

    assertTrue(CookieUtil.getCookieValue(request, "test3").isEmpty());
  }

  @Test
  void testGetCookieValueNoCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertTrue(CookieUtil.getCookieValue(request, "test3").isEmpty());
  }

  @Test
  void testDeleteCookie() {
    Cookie cookie1 = new Cookie("test", "testValue");
    cookie1.setMaxAge(180);

    Cookie cookie2 = new Cookie("test2", "testValue2");
    cookie2.setMaxAge(180);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(cookie1, cookie2);
    response.addCookie(cookie1);
    response.addCookie(cookie2);

    CookieUtil.deleteCookie(request, response, "test");

    Cookie responseCookie1 = Objects.requireNonNull(response.getCookie("test"));
    assertEquals("", responseCookie1.getValue());
    assertEquals(0, responseCookie1.getMaxAge());

    Cookie responseCookie2 = Objects.requireNonNull(response.getCookie("test2"));
    assertEquals("testValue2", responseCookie2.getValue());
    assertEquals(180, responseCookie2.getMaxAge());
  }

  @Test
  void testDeleteCookieNoCookie() {

    Cookie cookie2 = new Cookie("test2", "testValue2");
    cookie2.setMaxAge(180);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(cookie2);
    response.addCookie(cookie2);

    CookieUtil.deleteCookie(request, response, "test");

    assertNull(response.getCookie("test"));

    Cookie responseCookie2 = Objects.requireNonNull(response.getCookie("test2"));
    assertEquals("testValue2", responseCookie2.getValue());
    assertEquals(180, responseCookie2.getMaxAge());
  }
}
