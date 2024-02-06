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

package de.frachtwerk.essencium.backend.service.translation;

import static java.util.Objects.requireNonNull;

import de.frachtwerk.essencium.backend.model.TranslationFileType;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import de.frachtwerk.essencium.backend.util.StringUtils;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranslationFileService {

  // TODO: Refactor this service -> it shouldn't contain any HTTP-specific logic, especially no
  // ResponseEntity returns

  public static final String TRANSLATION_FILE_CACHE = "translation-file-cache";

  private static final String JSON_MIME_TYPE = MediaType.APPLICATION_JSON_VALUE;
  private static final String XLIFF_MIME_TYPE = "application/xliff+xml";
  private static final String ZIP_MIME_TYPE = "application/zip";
  private static final String PROPERTIES_MIME_TYPE = "text/x-java-properties";

  private final JsonTranslationFileCreator jsonTranslationFileCreator;
  private final ResourceBundleTranslationFileCreator resourceBundleTranslationFileCreator;
  private final XliffTranslationFileGenerator xliffTranslationFileGenerator;

  private final ResourceBundleParser resourceBundleParser;
  private final XliffParser xliffParser;
  private final TranslationService translationService;

  @Autowired
  public TranslationFileService(
      @NotNull final JsonTranslationFileCreator jsonTranslationFileCreator,
      @NotNull final ResourceBundleTranslationFileCreator resourceBundleTranslationFileCreator,
      @NotNull final XliffTranslationFileGenerator xliffTranslationFileGenerator,
      @NotNull final ResourceBundleParser resourceBundleParser,
      @NotNull final XliffParser xliffParser,
      @NotNull final TranslationService translationService) {
    this.jsonTranslationFileCreator = jsonTranslationFileCreator;
    this.resourceBundleTranslationFileCreator = resourceBundleTranslationFileCreator;
    this.xliffTranslationFileGenerator = xliffTranslationFileGenerator;
    this.resourceBundleParser = resourceBundleParser;
    this.xliffParser = xliffParser;
    this.translationService = translationService;
  }

  @CacheEvict(value = TRANSLATION_FILE_CACHE, allEntries = true)
  public void updateTranslations(
      @NotNull final MultipartFile multipartFile, @NotNull final Locale targetLocale) {
    final TranslationFileType fileType;
    try {
      fileType =
          TranslationFileType.from(
              StringUtils.getFileType(requireNonNull(multipartFile.getOriginalFilename())));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new TranslationFileException("Unable to determine translation file type.", e);
    }

    final TranslationFileParser suitingParser =
        switch (fileType) {
          case PROPERTIES -> resourceBundleParser;
          case XLIFF -> xliffParser;
          default -> throw new TranslationFileException("Unknown file type [" + fileType + "]");
        };

    try {
      translationService.updateTranslations(
          suitingParser.parse(multipartFile.getInputStream(), targetLocale));
    } catch (IOException e) {
      throw new TranslationFileException(e.getMessage());
    }
  }

  @NotNull
  public ResponseEntity<Resource> getTranslationFile(
      @NotNull final Locale locale, final boolean cache, @NotNull final TranslationFileType type) {
    return switch (type) {
      case JSON -> getJsonTranslationFile(locale, cache);
      case PROPERTIES, RESOURCEBUNDLE -> getResourceBundleTranslationFile(locale, cache);
      case XLIFF -> getXliffTranslationFile(locale, cache);
      default -> throw new IllegalStateException("Unexpected file type");
    };
  }

  @NotNull
  public ResponseEntity<Resource> getTranslationFile(
      final boolean cache, @NotNull final TranslationFileType type) {
    return switch (type) {
      case JSON -> getJsonTranslationFile(cache);
      case PROPERTIES, RESOURCEBUNDLE -> getResourceBundleTranslationFile(cache);
      case XLIFF -> getXliffTranslationFile(cache);
      default -> throw new IllegalStateException("Unexpected file type");
    };
  }

  @NotNull
  public ResponseEntity<Resource> getJsonTranslationFile(
      @NotNull final Locale locale, final boolean cache) {
    var translationFile = jsonTranslationFileCreator.createLocaleTranslationFile(locale, cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, JSON_MIME_TYPE);
    return ResponseEntity.ok()
        .headers(headers)
        .contentLength(translationFile.length)
        .body(new ByteArrayResource(translationFile, locale.toString()));
  }

  @NotNull
  public ResponseEntity<Resource> getJsonTranslationFile(final boolean cache) {
    var translationFile = jsonTranslationFileCreator.createGlobalTranslationFile(cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, JSON_MIME_TYPE);
    return ResponseEntity.ok()
        .headers(headers)
        .contentLength(translationFile.length)
        .body(new ByteArrayResource(translationFile));
  }

  @NotNull
  public ResponseEntity<Resource> getResourceBundleTranslationFile(
      @NotNull final Locale locale, final boolean cache) {
    var translationFile =
        resourceBundleTranslationFileCreator.createLocaleTranslationFile(locale, cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + locale + ".properties");
    headers.add(HttpHeaders.CONTENT_TYPE, PROPERTIES_MIME_TYPE);
    return ResponseEntity.ok()
        .contentLength(translationFile.length)
        .headers(headers)
        .body(new ByteArrayResource(translationFile));
  }

  @NotNull
  public ResponseEntity<Resource> getResourceBundleTranslationFile(final boolean cache) {

    var translationFile = resourceBundleTranslationFileCreator.createGlobalTranslationFile(cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.zip");
    headers.add(HttpHeaders.CONTENT_TYPE, ZIP_MIME_TYPE);
    return ResponseEntity.ok()
        .contentLength(translationFile.length)
        .headers(headers)
        .body(new ByteArrayResource(translationFile));
  }

  @NotNull
  public ResponseEntity<Resource> getXliffTranslationFile(
      @NotNull final Locale locale, final boolean cache) {
    var translationFile = xliffTranslationFileGenerator.createLocaleTranslationFile(locale, cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + locale + ".xliff");
    headers.add(HttpHeaders.CONTENT_TYPE, XLIFF_MIME_TYPE);
    return ResponseEntity.ok()
        .contentLength(translationFile.length)
        .headers(headers)
        .body(new ByteArrayResource(translationFile));
  }

  @NotNull
  public ResponseEntity<Resource> getXliffTranslationFile(final boolean cache) {
    var translationFile = xliffTranslationFileGenerator.createGlobalTranslationFile(cache);
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.xliff");
    headers.add(HttpHeaders.CONTENT_TYPE, XLIFF_MIME_TYPE);
    return ResponseEntity.ok()
        .contentLength(translationFile.length)
        .headers(headers)
        .body(new ByteArrayResource(translationFile));
  }
}
