/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
public class JsonTranslationFileCreator implements TranslationFileCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonTranslationFileCreator.class);

  private final ObjectMapper jsonMapper = new ObjectMapper();

  private final TranslationService translationService;

  @Autowired
  public JsonTranslationFileCreator(@NotNull final TranslationService translationService) {
    this.translationService = translationService;
  }

  @Override
  @Cacheable(
      value = TranslationFileService.TRANSLATION_FILE_CACHE,
      key = "'json-global'",
      condition = "#cache==true")
  public byte[] createGlobalTranslationFile(final boolean cache) {
    LOGGER.info("Creating global json file!");
    var translations = translationService.getTranslations();
    var groupedTranslations = TranslationFileUtil.groupByLocale(translations);

    var groupedObjectMaps =
        groupedTranslations.entrySet().stream()
            .map(
                localeCollectionEntry -> {
                  var objectMap = buildObjectMap(localeCollectionEntry.getValue());

                  return Pair.of(localeCollectionEntry.getKey(), objectMap);
                })
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    byte[] translationFile;

    try {
      translationFile = jsonMapper.writeValueAsBytes(groupedObjectMaps);
    } catch (JsonProcessingException e) {
      throw new TranslationFileException("Unable to create JSON-File", e);
    }

    return translationFile;
  }

  @Override
  @Cacheable(
      value = TranslationFileService.TRANSLATION_FILE_CACHE,
      key = "'json-local_' + #locale.toString()",
      condition = "#cache==true")
  public byte[] createLocaleTranslationFile(final @NotNull Locale locale, final boolean cache) {
    LOGGER.info("Creating json file for locale [{}]", locale);

    var translations = translationService.getTranslations(locale);
    var translationMap = buildObjectMap(translations);

    byte[] translationFile;

    try {
      translationFile = jsonMapper.writeValueAsBytes(translationMap);
    } catch (JsonProcessingException e) {
      throw new TranslationFileException("Unable to create JSON-File", e);
    }

    return translationFile;
  }

  @NotNull
  private Map<String, Object> buildObjectMap(@NotNull final Collection<Translation> translations) {
    final var finalMap = new HashMap<String, Object>();
    final var processMap = new HashMap<String, Collection<Translation>>();
    translations.forEach(
        translation -> {
          final var key = translation.getKey();
          final var value = translation.getValue();
          if (translation.getKey().contains(".")) {
            final var firstDot = key.indexOf(".");
            final var headKey = key.substring(0, firstDot);
            final var tailKey = key.substring(firstDot + 1);
            processMap.putIfAbsent(headKey, new LinkedList<>());
            processMap.get(headKey).add(new Translation(translation.getLocale(), tailKey, value));
          } else {
            finalMap.put(key, translation.getValue());
          }
        });

    processMap.forEach((s, translations1) -> finalMap.put(s, buildObjectMap(translations1)));

    return finalMap;
  }
}
