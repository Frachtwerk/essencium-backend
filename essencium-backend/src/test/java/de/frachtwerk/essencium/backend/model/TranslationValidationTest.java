/*
 * Copyright (C) 2026-7613 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.dto.TranslationDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Bean Validation forbids constraints on parameters of overriding methods (e.g. {@code compareTo}).
 * Hibernate Validator reports such declarations with HV000151 as soon as it builds the metadata of
 * the class, which broke every insert of a {@link Translation}.
 */
class TranslationValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void translationCanBeValidated() {
    Translation translation =
        Translation.builder().locale(Locale.GERMAN).key("key").value("value").build();

    assertThat(validator.validate(translation)).isEmpty();
  }

  @Test
  void translationDtoCanBeValidated() {
    TranslationDto dto = new TranslationDto(Locale.GERMAN, "key", "value");

    assertThat(validator.validate(dto)).isEmpty();
  }
}
