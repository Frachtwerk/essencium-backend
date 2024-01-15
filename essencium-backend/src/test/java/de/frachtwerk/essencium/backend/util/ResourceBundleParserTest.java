/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.util;

import de.frachtwerk.essencium.backend.service.translation.ResourceBundleParser;
import de.frachtwerk.essencium.backend.service.translation.TranslationFileParser;
import java.util.Locale;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourceBundleParserTest {

  private final TranslationFileParser testSubject = new ResourceBundleParser();

  @Test
  void parse() {
    var testLocale = Locale.CANADA_FRENCH;
    var fileToTranslate =
        this.getClass()
            .getClassLoader()
            .getResourceAsStream("testfiles/TranslationResourceBundle.properties");

    var retVal = testSubject.parse(fileToTranslate, testLocale);

    Assertions.assertThat(retVal).isNotEmpty();
    retVal.forEach(
        translation -> {
          Assertions.assertThat(translation.getLocale()).isEqualTo(testLocale);
          Assertions.assertThat(translation.getKey()).isNotNull();
          Assertions.assertThat(translation.getValue()).isNotNull();
        });
  }
}
