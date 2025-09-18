/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

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
