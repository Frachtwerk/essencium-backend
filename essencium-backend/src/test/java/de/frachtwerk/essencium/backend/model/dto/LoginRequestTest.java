package de.frachtwerk.essencium.backend.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginRequestTest {
  private Validator validator;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void allFieldsSetCorrectly() {
    LoginRequest loginRequest = new LoginRequest("devnull@frachtwerk.de", "password");
    Set<ConstraintViolation<LoginRequest>> constraintViolations = validator.validate(loginRequest);
    assertTrue(constraintViolations.isEmpty());
  }

  @Test
  void mandatoryFieldsNullTest() {
    LoginRequest loginRequest = new LoginRequest(null, null);
    Set<ConstraintViolation<LoginRequest>> constraintViolations = validator.validate(loginRequest);
    assertEquals(2, constraintViolations.size());
  }

  @Test
  void mandatoryFieldsInvalidTest() {
    LoginRequest loginRequest = new LoginRequest("abc", "  ");
    Set<ConstraintViolation<LoginRequest>> constraintViolations = validator.validate(loginRequest);
    assertEquals(2, constraintViolations.size());
  }
}
