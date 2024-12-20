/*
 *
 *  * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *  *
 *  * This file is part of essencium-backend.
 *  *
 *  * essencium-backend is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * essencium-backend is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.frachtwerk.essencium.backend.test.integration.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.frachtwerk.essencium.backend.model.exception.EssenciumException;
import de.frachtwerk.essencium.backend.model.exception.EssenciumRuntimeException;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
public class ExceptionHandlingIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @MethodSource("provideParameters")
  void handleExceptions(String apiUrl, String exceptionName, String exceptionMessage)
      throws Exception {

    mockMvc
        .perform(get(apiUrl))
        .andExpect(status().is(500))
        .andExpect(jsonPath("$.internal.internalErrorType").value(exceptionName))
        .andExpect(jsonPath("$.internal.internalErrorMessage").value(exceptionMessage))
        .andExpect(jsonPath("$.debug.stackTrace").isNotEmpty());
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(
            "/essencium",
            EssenciumException.class.getSimpleName(),
            TestExceptionController.ESSENCIUM_EXCEPTION_MESSAGE),
        Arguments.of(
            "/essencium-runtime",
            EssenciumRuntimeException.class.getSimpleName(),
            TestExceptionController.ESSENCIUM_RUNTIME_EXCEPTION_MESSAGE),
        Arguments.of(
            "/java",
            Exception.class.getSimpleName(),
            TestExceptionController.JAVA_EXCEPTION_MESSAGE),
        Arguments.of(
            "/java-runtime",
            RuntimeException.class.getSimpleName(),
            TestExceptionController.JAVA_RUNTIME_EXCEPTION_MESSAGE));
  }
}
