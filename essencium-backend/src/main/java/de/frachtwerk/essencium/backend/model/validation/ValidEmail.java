package de.frachtwerk.essencium.backend.model.validation;

import jakarta.validation.Constraint;
import java.lang.annotation.*;

/** The annotated element must be a valid email address. */
@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidEmail {
  String message() default "Invalid email address";

  Class<?>[] groups() default {};

  Class<?>[] payload() default {};
}
