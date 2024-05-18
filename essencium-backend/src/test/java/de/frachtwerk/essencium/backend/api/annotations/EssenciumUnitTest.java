package de.frachtwerk.essencium.backend.api.annotations;

import de.frachtwerk.essencium.backend.api.data.extension.MetricCleanUpExtension;
import de.frachtwerk.essencium.backend.api.data.extension.TestObjectInjectionExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The EssenciumUnitTest annotation should be used on all jUnit test classes in the Essencium
 * project. It enables the jUnit extensions {@link TestObjectInjectionExtension} and {@link
 * MetricCleanUpExtension}, which provides utility functions to create test objects and interact
 * with mocks. Especially the {@link TestObjectInjectionExtension} enables the possibility to create
 * objects via parameter injection. See {@link
 * TestObjectInjectionExtension#resolveParameter(ParameterContext, ExtensionContext)} for more
 * information.
 */
@ExtendWith(TestObjectInjectionExtension.class)
@ExtendWith(MetricCleanUpExtension.class)
@ExtendWith(MockitoExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EssenciumUnitTest {}
