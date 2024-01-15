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

import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class XliffTranslationFileGenerator implements TranslationFileCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(XliffTranslationFileGenerator.class);

  private static final Locale DEFAULT_SOURCE_LOCALE = Locale.US;

  private final TranslationService translationService;

  @Autowired
  public XliffTranslationFileGenerator(@NotNull final TranslationService translationService) {
    this.translationService = translationService;
  }

  @Override
  @Cacheable(value = TRANSLATION_FILE_CACHE, key = "'xliff-global'", condition = "#cache==true")
  public byte[] createGlobalTranslationFile(final boolean cache) {
    LOGGER.info("Creating xliff-global file!");

    var locales = translationService.getAvailableLocales();
    var byteArrayOutputStream = new ByteArrayOutputStream();

    try (byteArrayOutputStream;
        var streamWriter = new OutputStreamWriter(byteArrayOutputStream);
        var writer = new XLIFFWriter()) {
      writer.create(streamWriter, DEFAULT_SOURCE_LOCALE.getLanguage());
      locales.forEach(
          locale -> {
            writer.writeStartFile(new StartFileData(locale.getLanguage()));
            getUnits(null, locale).forEach(writer::writeUnit);
            writer.writeEndFile();
          });
    } catch (IOException e) {
      throw new TranslationFileException("Unable to create creation file!", e);
    }

    return byteArrayOutputStream.toByteArray();
  }

  @Override
  @Cacheable(
      value = TRANSLATION_FILE_CACHE,
      key = "'xliff-local_' + #locale.toString()",
      condition = "#cache==true")
  public byte[] createLocaleTranslationFile(@NotNull final Locale locale, final boolean cache) {
    LOGGER.info("Creating xliff file for locale [{}]", locale);

    final var units = getUnits(DEFAULT_SOURCE_LOCALE, locale);
    final var byteArrayOutputStream = new ByteArrayOutputStream();

    try (byteArrayOutputStream;
        final var streamWriter = new OutputStreamWriter(byteArrayOutputStream);
        final var writer = new XLIFFWriter()) {
      writer.create(streamWriter, DEFAULT_SOURCE_LOCALE.getLanguage(), locale.getLanguage());
      writer.writeStartFile(new StartFileData(locale.getLanguage()));
      units.forEach(writer::writeUnit);

    } catch (IOException e) {
      throw new TranslationFileException("Unable to create creation file!", e);
    }

    return byteArrayOutputStream.toByteArray();
  }

  @NotNull
  private Collection<Unit> getUnits(
      @Nullable final Locale sourceLocale, @NotNull final Locale targetLocale) {
    final var translationMaps = translationService.getTranslationMaps();
    final var sortedTranslationMaps = new TreeMap<>(translationMaps);

    final var extractedUnits = new HashMap<String, Unit>();

    sortedTranslationMaps.keySet().stream()
        .map(key -> key.split("\\.", 2)[0])
        .forEach(keyHead -> extractedUnits.putIfAbsent(keyHead, new Unit(keyHead)));

    sortedTranslationMaps.forEach(
        (key, value) -> {
          final var splitKey = key.split("\\.", 2);
          final var head = splitKey[0];
          final var tail = splitKey.length == 2 ? splitKey[1] : null;

          final var unit = extractedUnits.get(head);

          final var segment = unit.appendSegment();
          final var targetTranslation = value.getOrDefault(targetLocale, "");
          if (sourceLocale == null) {
            segment.setSource(targetTranslation);
          } else {
            final var sourceTranslation = value.getOrDefault(sourceLocale, "");

            segment.setSource(sourceTranslation);
            segment.setTarget(targetTranslation);
          }

          if (tail != null) {
            segment.setId(tail);
          }
        });

    return extractedUnits.values();
  }
}
