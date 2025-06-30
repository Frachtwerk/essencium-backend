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

import de.frachtwerk.essencium.backend.model.exception.EssenciumException;
import de.frachtwerk.essencium.backend.model.exception.EssenciumRuntimeException;
import de.frachtwerk.essencium.backend.test.integration.util.exceptions.ExceptionsDummyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TestExceptionController {

  public static final String ESSENCIUM_EXCEPTION_MESSAGE = "Test Essencium Exception";
  public static final String ESSENCIUM_RUNTIME_EXCEPTION_MESSAGE =
      "Test Essencium Runtime Exception";
  public static final String JAVA_EXCEPTION_MESSAGE = "Test Java Exception";
  public static final String JAVA_RUNTIME_EXCEPTION_MESSAGE = "Test Java Runtime Exception";

  @GetMapping("/essencium")
  public void essenciumException() throws EssenciumException {
    throw new EssenciumException(ESSENCIUM_EXCEPTION_MESSAGE);
  }

  @GetMapping("/essencium-runtime")
  public void essenciumRuntimeException() {
    throw new EssenciumRuntimeException(ESSENCIUM_RUNTIME_EXCEPTION_MESSAGE);
  }

  @GetMapping("/java")
  public void javaException() throws Exception {
    throw new Exception(JAVA_EXCEPTION_MESSAGE);
  }

  @GetMapping("/java-runtime")
  public void javaRuntimeException() {
    throw new RuntimeException(JAVA_RUNTIME_EXCEPTION_MESSAGE);
  }

  @PostMapping("/method-argument-invalid")
  public void methodArgumentInvalid(@Valid @RequestBody ExceptionsDummyDto exceptionsDummyDto) {}

  @GetMapping("/handler-method-validation")
  public String handlerMethodValidation(@Min(1) @RequestParam int id) {
    return "OK";
  }
}
