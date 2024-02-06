package de.frachtwerk.essencium.backend.model.validation;

import de.frachtwerk.essencium.backend.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

  @Override
  public void initialize(ValidEmail constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return StringUtils.isValidEmailAddress(value);
  }
}
