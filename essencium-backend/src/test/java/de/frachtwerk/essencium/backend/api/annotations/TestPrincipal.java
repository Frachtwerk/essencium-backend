package de.frachtwerk.essencium.backend.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface TestPrincipal {
  TestPrincipleType type() default TestPrincipleType.LOGGED_IN;
}
