package de.frachtwerk.essencium.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
          "test.@example.com",
          "firstname+lastname@example.com",
          "1234567890@example.com",
          "email@example-one.com",
          "_______@example.com",
          "jon.o'conner@example.com",
          "#@example.com",
          "abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ/*-+@local_part_exactly_64_char.com",
          "domain.name.exactly.255.chars@abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.com");

  private static final List<String> INVALID_EMAIL_ADDRESSES =
      List.of(
          "test",
          "test@localhost",
          "test@example..com",
          "email@123.123.123.123",
          "email@[123.123.123.123]",
          "email@example.com (Joe Smith)",
          "email\"@example.com",
          "empty space@example.com",
          "email@-example.com",
          "<email>@example.com",
          ";email@example.com",
          "#@%^%#$@#$@#.com",
          "email@.example.com",
          "email@first@second.com",
          "@example.com",
          "",
          "abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ/*-+_-@local_part_too_long.com",
          "domain.name.exactly.256.chars@1abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.abcdefghijklmnopqrstuvw1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ63c.com");

  private Validator validator;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

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

  @Test
  void isValidEmailAddressNull() {
    assertFalse(StringUtils.isValidEmailAddress(null));
  }

  @Test
  void isValidEmailAddressAnnotation() {
    for (String validEmailAddress : VALID_EMAIL_ADDRESSES) {
      Set<ConstraintViolation<TestEmail>> constraintViolations =
          validator.validate(new TestEmail(validEmailAddress));
      assertThat(constraintViolations).isEmpty();
    }
  }

  @Test
  void isInvalidEmailAddressAnnotation() {
    for (String invalidEmailAddress : INVALID_EMAIL_ADDRESSES) {
      Set<ConstraintViolation<TestEmail>> constraintViolations =
          validator.validate(new TestEmail(invalidEmailAddress));
      assertThat(constraintViolations).isNotEmpty();
    }
  }

  @Test
  void isInvalidEmailAddressAnnotationNull() {
    Set<ConstraintViolation<TestEmail>> constraintViolations =
        validator.validate(new TestEmail(null));
    assertThat(constraintViolations).isNotEmpty();
  }
}
