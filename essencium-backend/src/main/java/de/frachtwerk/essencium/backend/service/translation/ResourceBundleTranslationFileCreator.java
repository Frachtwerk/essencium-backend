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

import static de.frachtwerk.essencium.backend.service.translation.TranslationFileService.TRANSLATION_FILE_CACHE;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ResourceBundleTranslationFileCreator implements TranslationFileCreator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResourceBundleTranslationFileCreator.class);

  private static final int ESTIMATED_CHARACTER_PER_TRANSLATION = 30;

  private final TranslationService translationService;

  @Autowired
  public ResourceBundleTranslationFileCreator(
      @NotNull final TranslationService translationService) {
    this.translationService = translationService;
  }

  @Override
  @Cacheable(
      value = TRANSLATION_FILE_CACHE,
      key = "'resource-bundle-global'",
      condition = "#cache==true")
  public byte[] createGlobalTranslationFile(final boolean cache) {
    LOGGER.info("Creating global resource-bundle file!");

    var translations = translationService.getTranslations();
    var groupedTranslations = TranslationFileUtil.groupByLocale(translations);
    final File zipFile;
    try {
      var tempDir =
          Files.createTempDirectory("translationFiles-" + OffsetDateTime.now().toEpochSecond());
      var tempTranslationFiles =
          groupedTranslations.entrySet().stream()
              .map(
                  localeCollectionEntry ->
                      Map.entry(
                          localeCollectionEntry.getKey(),
                          singleBundleContentString(localeCollectionEntry.getValue())))
              .map(
                  localeStringEntry -> {
                    final File tempLocaleFile;
                    final FileOutputStream tempLocalFileStream;
                    try {
                      tempLocaleFile =
                          File.createTempFile(
                              "translation-" + localeStringEntry.getKey().toString(),
                              ".properties",
                              tempDir.toFile());
                      tempLocalFileStream = new FileOutputStream(tempLocaleFile);
                      tempLocalFileStream.write(localeStringEntry.getValue().getBytes());
                      tempLocalFileStream.close();
                      return tempLocaleFile;
                    } catch (IOException e) {
                      throw new RuntimeException();
                    }
                  })
              .toList();

      zipFile = File.createTempFile("translation-archive", ".zip");
      try (var zipStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
        for (final var file : tempTranslationFiles) {
          try (var fileStream = new FileInputStream(file)) {
            zipStream.putNextEntry(new ZipEntry(file.getName()));
            zipStream.write(fileStream.readAllBytes());
          } catch (IOException ex) {
            LOGGER.warn(
                "failed to add file {} to archive {}",
                file.getAbsolutePath(),
                zipFile.getAbsoluteFile());
          }
        }
      }
    } catch (IOException e) {
      throw new TranslationFileException(e.getMessage());
    }

    final byte[] zipBytes;

    try (var zipFileStream = new FileInputStream(zipFile)) {
      zipBytes = zipFileStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException();
    }

    return zipBytes;
  }

  @Override
  @Cacheable(
      value = TRANSLATION_FILE_CACHE,
      key = "'resource-bundle-local_' + #locale.toString()",
      condition = "#cache==true")
  public byte[] createLocaleTranslationFile(final @NotNull Locale locale, final boolean cache) {
    LOGGER.info("Creating resource bundle file for locale [{}]", locale);

    var translations = translationService.getTranslations(locale);
    var contentString = singleBundleContentString(translations);

    return contentString.getBytes();
  }

  private String singleBundleContentString(Collection<Translation> translations) {
    var contentBuilder =
        new StringBuilder(ESTIMATED_CHARACTER_PER_TRANSLATION * translations.size());

    translations.stream()
        .sorted(Comparator.comparing(Translation::getKey))
        .forEach(
            translation ->
                contentBuilder
                    .append(translation.getKey())
                    .append("=")
                    .append(translation.getValue())
                    .append("\n"));
    return contentBuilder.toString();
  }
}
