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

package de.frachtwerk.essencium.backend.service.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ResourceBundleTranslationFileCreatorTest {

  private TranslationService translationServiceMock;
  private ResourceBundleTranslationFileCreator testSubject;

  @BeforeEach
  void setUp() {
    translationServiceMock = mock(TranslationService.class);
    testSubject = new ResourceBundleTranslationFileCreator(translationServiceMock);
  }

  @Test
  void createLocaleTranslationFile_happyPath_returnsPropertiesContent() {
    var locale = Locale.ENGLISH;
    var t1 = new Translation(locale, "app.title", "My App");
    var t2 = new Translation(locale, "app.greeting", "Hello");
    when(translationServiceMock.getTranslations(locale)).thenReturn(List.of(t1, t2));

    byte[] result = testSubject.createLocaleTranslationFile(locale, false);

    assertThat(result).isNotNull().isNotEmpty();
    String content = new String(result);
    // keys must be sorted alphabetically
    assertThat(content).contains("app.greeting=Hello\n");
    assertThat(content).contains("app.title=My App\n");
    assertThat(content.indexOf("app.greeting")).isLessThan(content.indexOf("app.title"));
  }

  @Test
  void createLocaleTranslationFile_emptyTranslations_returnsEmptyBytes() {
    var locale = Locale.GERMAN;
    when(translationServiceMock.getTranslations(locale)).thenReturn(Collections.emptyList());

    byte[] result = testSubject.createLocaleTranslationFile(locale, false);

    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void createLocaleTranslationFile_singleTranslation_returnsCorrectFormat() {
    var locale = Locale.FRENCH;
    var translation = new Translation(locale, "key.one", "valeur");
    when(translationServiceMock.getTranslations(locale)).thenReturn(List.of(translation));

    byte[] result = testSubject.createLocaleTranslationFile(locale, false);

    assertThat(new String(result)).isEqualTo("key.one=valeur\n");
  }

  @Test
  void createLocaleTranslationFile_translationWithNullValue_includesNullLiteral() {
    var locale = Locale.ENGLISH;
    var translation = new Translation(locale, "key.null", null);
    when(translationServiceMock.getTranslations(locale)).thenReturn(List.of(translation));

    // Translation.getValue() returns null; String concatenation will produce "null"
    byte[] result = testSubject.createLocaleTranslationFile(locale, false);

    assertThat(new String(result)).isEqualTo("key.null=null\n");
  }

  @Test
  void createLocaleTranslationFile_nullLocale_returnsEmptyBytes() {
    // @NotNull is a validation annotation only — no runtime enforcement; service returns empty list
    when(translationServiceMock.getTranslations(null)).thenReturn(Collections.emptyList());

    byte[] result = testSubject.createLocaleTranslationFile(null, false);

    assertThat(result).isNotNull().isEmpty();
  }

  // ---- createGlobalTranslationFile ----

  @Test
  void createGlobalTranslationFile_happyPath_returnsValidZip() throws Exception {
    var enLocale = Locale.ENGLISH;
    var deLocale = Locale.GERMAN;
    var t1 = new Translation(enLocale, "app.title", "My App");
    var t2 = new Translation(deLocale, "app.title", "Meine App");
    when(translationServiceMock.getTranslations()).thenReturn(List.of(t1, t2));

    byte[] result = testSubject.createGlobalTranslationFile(false);

    assertThat(result).isNotNull().isNotEmpty();
    // verify it is a valid ZIP
    try (var zip = new ZipInputStream(new ByteArrayInputStream(result))) {
      var entries = new java.util.ArrayList<String>();
      java.util.zip.ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entries.add(entry.getName());
        zip.closeEntry();
      }
      assertThat(entries).hasSize(2);
      assertThat(entries).allMatch(name -> name.endsWith(".properties"));
    }
  }

  @Test
  void createGlobalTranslationFile_emptyTranslations_returnsEmptyZip() throws Exception {
    when(translationServiceMock.getTranslations()).thenReturn(Collections.emptyList());

    byte[] result = testSubject.createGlobalTranslationFile(false);

    assertThat(result).isNotNull().isNotEmpty();
    try (var zip = new ZipInputStream(new ByteArrayInputStream(result))) {
      assertThat(zip.getNextEntry()).isNull();
    }
  }

  @Test
  void createGlobalTranslationFile_multipleTranslationsSameLocale_groupedIntoOneFile()
      throws Exception {
    var locale = Locale.ENGLISH;
    var t1 = new Translation(locale, "b.key", "B");
    var t2 = new Translation(locale, "a.key", "A");
    when(translationServiceMock.getTranslations()).thenReturn(List.of(t1, t2));

    byte[] result = testSubject.createGlobalTranslationFile(false);

    try (var zip = new ZipInputStream(new ByteArrayInputStream(result))) {
      var entry = zip.getNextEntry();
      assertThat(entry).isNotNull();
      String content = new String(zip.readAllBytes());
      // one entry for locale, keys sorted
      assertThat(content).contains("a.key=A\n");
      assertThat(content).contains("b.key=B\n");
      assertThat(content.indexOf("a.key")).isLessThan(content.indexOf("b.key"));
      zip.closeEntry();
      assertThat(zip.getNextEntry()).isNull();
    }
  }

  @Test
  void createGlobalTranslationFile_serviceThrowsRuntimeException_propagatesException() {
    when(translationServiceMock.getTranslations())
        .thenThrow(new RuntimeException("DB unavailable"));

    assertThatThrownBy(() -> testSubject.createGlobalTranslationFile(false))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("DB unavailable");
  }

  @Test
  void createGlobalTranslationFile_tempDirectoryCreationFails_throwsTranslationFileException() {
    when(translationServiceMock.getTranslations())
        .thenReturn(List.of(new Translation(Locale.ENGLISH, "k", "v")));

    try (MockedStatic<Files> mockedFiles =
        Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
      mockedFiles
          .when(() -> Files.createTempDirectory(anyString()))
          .thenThrow(new IOException("disk full"));

      assertThatThrownBy(() -> testSubject.createGlobalTranslationFile(false))
          .isInstanceOf(TranslationFileException.class)
          .hasMessageContaining("Failed to create global resource-bundle archive")
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Test
  void createGlobalTranslationFile_localeTempFileCreationFails_throwsTranslationFileException() {
    when(translationServiceMock.getTranslations())
        .thenReturn(List.of(new Translation(Locale.ENGLISH, "k", "v")));

    try (MockedStatic<File> mockedFile =
        Mockito.mockStatic(File.class, Mockito.CALLS_REAL_METHODS)) {
      // 3-arg createTempFile is used for per-locale files
      mockedFile
          .when(() -> File.createTempFile(anyString(), eq(".properties"), any(File.class)))
          .thenThrow(new IOException("no space left"));

      assertThatThrownBy(() -> testSubject.createGlobalTranslationFile(false))
          .isInstanceOf(TranslationFileException.class)
          .hasMessageContaining("Failed to create temporary translation file for locale")
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Test
  void createGlobalTranslationFile_zipFileCreationFails_throwsTranslationFileException() {
    // Empty translations → no locale temp files created → zip archive creation is the first
    // File.createTempFile call, making it straightforward to trigger
    when(translationServiceMock.getTranslations()).thenReturn(Collections.emptyList());

    try (MockedStatic<File> mockedFile =
        Mockito.mockStatic(File.class, Mockito.CALLS_REAL_METHODS)) {
      // 2-arg createTempFile is used for the zip archive
      mockedFile
          .when(() -> File.createTempFile(eq("translation-archive"), eq(".zip")))
          .thenThrow(new IOException("no space left"));

      assertThatThrownBy(() -> testSubject.createGlobalTranslationFile(false))
          .isInstanceOf(TranslationFileException.class)
          .hasMessageContaining("Failed to create global resource-bundle archive")
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Test
  void createGlobalTranslationFile_zipContainsCorrectEntryContent() throws Exception {
    var locale = Locale.ENGLISH;
    var translation = new Translation(locale, "hello", "world");
    when(translationServiceMock.getTranslations()).thenReturn(List.of(translation));

    byte[] result = testSubject.createGlobalTranslationFile(false);

    try (var zip = new ZipInputStream(new ByteArrayInputStream(result))) {
      var entry = zip.getNextEntry();
      assertThat(entry).isNotNull();
      String content = new String(zip.readAllBytes());
      assertThat(content).isEqualTo("hello=world\n");
    }
  }

  @Test
  void createLocaleTranslationFile_keysAreSortedAlphabetically() {
    var locale = Locale.ENGLISH;
    var translations =
        List.of(
            new Translation(locale, "z.last", "Z"),
            new Translation(locale, "a.first", "A"),
            new Translation(locale, "m.middle", "M"));
    when(translationServiceMock.getTranslations(locale)).thenReturn(translations);

    String content = new String(testSubject.createLocaleTranslationFile(locale, false));

    int idxA = content.indexOf("a.first");
    int idxM = content.indexOf("m.middle");
    int idxZ = content.indexOf("z.last");
    assertThat(idxA).isLessThan(idxM);
    assertThat(idxM).isLessThan(idxZ);
  }
}
