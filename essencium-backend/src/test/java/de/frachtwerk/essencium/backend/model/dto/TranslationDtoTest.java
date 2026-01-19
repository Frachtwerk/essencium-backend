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

package de.frachtwerk.essencium.backend.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationDtoTest {

  @Test
  void compareTo_shouldReturnZero_whenKeysAreEqual() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("testKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("testKey").value("Value2").build();

    assertEquals(0, dto1.compareTo(dto2));
  }

  @Test
  void compareTo_shouldReturnNegative_whenKeyIsLessThanOther() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("aKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("bKey").value("Value2").build();

    assertTrue(dto1.compareTo(dto2) < 0);
  }

  @Test
  void compareTo_shouldReturnPositive_whenKeyIsGreaterThanOther() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("zKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("aKey").value("Value2").build();

    assertTrue(dto1.compareTo(dto2) > 0);
  }
}
