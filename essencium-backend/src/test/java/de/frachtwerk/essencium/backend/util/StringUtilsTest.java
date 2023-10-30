package de.frachtwerk.essencium.backend.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

  private static final List<String> VALID_EMAIL_ADDRESSES =
      List.of(
          "test@example.com",
          "test.user@example.com",
          "test.user.child@example.com",
          "test@example.host.com",
          "test.user.child@example.host.parent.123.com",
          "test.user.123.child@example.host.parent.123.com",
          "test..user_child@example.host.parent.123.com",
          "test@localhost",
          "test.@example.com",
          "firstname+lastname@example.com",
          "1234567890@example.com",
          "email@example-one.com",
          "_______@example.com",
          "jon.o'conner@example.com",
          "#@example.com");

  private static final List<String> INVALID_EMAIL_ADDRESSES =
      List.of(
          "test",
          "test@example..com",
          "email@123.123.123.123",
          "email@[123.123.123.123]",
          "email\"@example.com",
          "empty space@example.com",
          "email@-example.com",
          "<email>@example.com",
          ";email@example.com",
          "#@%^%#$@#$@#.com");

  @Test
  void isValidEmailAddress() {
    for (String validEmailAddress : VALID_EMAIL_ADDRESSES) {
      assertTrue(StringUtils.isValidEmailAddress(validEmailAddress));
    }
  }

  @Test
  void isInvalidEmailAddress() {
    for (String invalidEmailAddress : INVALID_EMAIL_ADDRESSES) {
      assertFalse(StringUtils.isValidEmailAddress(invalidEmailAddress));
    }
  }
}
