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

package de.frachtwerk.essencium.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.TranslationFileType;
import de.frachtwerk.essencium.backend.model.dto.TranslationDto;
import de.frachtwerk.essencium.backend.service.translation.TranslationFileService;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

class TranslationControllerTest {

  private final TranslationService translationServiceMock = mock(TranslationService.class);
  private final TranslationFileService translationFileServiceMock =
      mock(TranslationFileService.class);

  private final TranslationController testSubject =
      new TranslationController(translationServiceMock, translationFileServiceMock);

  @Nested
  class GetTranslationFile {

    @Test
    void defaultTranslationFile() {
      var testLocale = mock(Locale.class);
      var testFileType = TranslationFileType.from("json");
      var testResource = mock(ResponseEntity.class);

      when(translationFileServiceMock.getTranslationFile(testLocale, false, testFileType))
          .thenReturn(testResource);

      assertThat(testSubject.getTranslationFile(testLocale, testFileType)).isSameAs(testResource);
    }

    @Test
    void extendedTranslationFile() {
      var testFileType = TranslationFileType.from("json");
      var testResource = mock(ResponseEntity.class);

      when(translationFileServiceMock.getTranslationFile(false, testFileType))
          .thenReturn(testResource);

      assertThat(testSubject.getTranslationFile(testFileType)).isSameAs(testResource);
    }
  }

  @Test
  void updateSingleTranslation() {
    var testLocale = mock(Locale.class);
    var testKey = "TEST_KEY";
    var testValue = "TEST_VALUE";

    var result = mock(Translation.class);

    when(translationServiceMock.updateTranslation(any()))
        .thenAnswer(
            invocationOnMock -> {
              var translation = invocationOnMock.getArgument(0, Translation.class);

              assertThat(translation.getLocale()).isEqualTo(testLocale);
              assertThat(translation.getKey()).isEqualTo(testKey);
              assertThat(translation.getValue()).isEqualTo(testValue);

              return result;
            });

    assertThat(testSubject.updateSingleTranslation(testLocale, testKey, testValue))
        .isSameAs(result);
  }

  @Test
  void updateTranslation() {
    final TranslationDto input = new TranslationDto(Locale.GERMAN, "key", "value");

    when(translationServiceMock.updateTranslation(any(Translation.class)))
        .thenAnswer(i -> i.getArgument(0));

    assertThat(testSubject.updateTranslation(input))
        .isEqualTo(new Translation(input.getLocale(), input.getKey(), input.getValue()));
  }

  @Test
  void updateTranslationMap() {
    var testLocale = mock(Locale.class);
    var testMap = new HashMap<String, Object>();

    var result = new LinkedList<Translation>();

    when(translationServiceMock.updateTranslation(testLocale, testMap)).thenReturn(result);

    assertThat(testSubject.updateTranslation(testLocale, testMap)).isSameAs(result);
  }

  @Test
  void deleteTranslation() {
    var testKey = "test.key";
    testSubject.deleteTranslation(testKey);
    Mockito.verify(translationServiceMock).deleteTranslation(testKey);
  }
}
