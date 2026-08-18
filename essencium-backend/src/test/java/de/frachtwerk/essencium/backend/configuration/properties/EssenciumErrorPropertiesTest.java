/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EssenciumErrorPropertiesTest {

  private static Set<ConstraintViolation<EssenciumErrorProperties>> validate(String urnPrefix) {
    EssenciumErrorProperties properties = new EssenciumErrorProperties();
    properties.setUrnPrefix(urnPrefix);

    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      return validator.validate(properties);
    }
  }

  @Test
  void acceptsTheDefaultPrefix() {
    assertThat(validate(new EssenciumErrorProperties().getUrnPrefix())).isEmpty();
  }

  @Test
  void rejectsABlankPrefix() {
    assertThat(validate("  ")).isNotEmpty();
  }

  @Test
  void rejectsAPrefixThatCannotFormAUri() {
    assertThat(validate("urn:frachtwerk error:")).isNotEmpty();
  }
}
