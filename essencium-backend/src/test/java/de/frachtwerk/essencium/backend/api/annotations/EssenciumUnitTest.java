package de.frachtwerk.essencium.backend.api.annotations;

import de.frachtwerk.essencium.backend.api.data.extension.MetricCleanUpExtension;
import de.frachtwerk.essencium.backend.api.data.extension.TestObjectInjectionExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestObjectInjectionExtension.class)
@ExtendWith(MetricCleanUpExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EssenciumUnitTest {}
