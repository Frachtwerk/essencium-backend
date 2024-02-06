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
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

  private final TranslationRepository translationRepository;

  @Autowired
  public TranslationService(@NotNull final TranslationRepository translationRepository) {
    this.translationRepository = translationRepository;
  }

  public Collection<Translation> getTranslations() {
    return translationRepository.findAll();
  }

  public Collection<Translation> getTranslations(@NotNull final Locale locale) {
    return translationRepository.findAllByLocale(locale);
  }

  public Optional<String> translate(String key, Locale locale) {
    return translateVariant(key, locale, 0);
  }

  // Inspired https://kazupon.github.io/vue-i18n/guide/pluralization.html
  public Optional<String> translateVariant(String key, Locale locale, int variantIndex) {
    final var translation = translationRepository.findByKeyAndLocale(key, locale);
    return translation
        .map(Translation::getValue)
        .flatMap(s -> Stream.of(s.split("\\|")).skip(variantIndex).findFirst())
        .map(String::trim);
  }

  @CacheEvict(value = TRANSLATION_FILE_CACHE, allEntries = true)
  public Translation updateTranslation(@NotNull final Translation translation) {
    return translationRepository.save(translation);
  }

  @CacheEvict(value = TRANSLATION_FILE_CACHE, allEntries = true)
  public Collection<Translation> updateTranslations(
      @NotNull final Collection<Translation> translations) {
    return translationRepository.saveAll(translations);
  }

  @CacheEvict(value = TRANSLATION_FILE_CACHE, allEntries = true)
  public Collection<Translation> updateTranslation(
      @NotNull final Locale locale, Map<String, Object> translationObjects) {
    final Collection<Pair<String, String>> keyValueList = parseKeyValueList("", translationObjects);

    final Collection<Translation> translations =
        keyValueList.stream()
            .map(
                translationPair ->
                    new Translation(
                        locale, translationPair.getFirst(), translationPair.getSecond()))
            .collect(Collectors.toCollection(LinkedList::new));
    return translationRepository.saveAll(translations);
  }

  public void deleteTranslation(String key) {
    translationRepository.deleteAll(translationRepository.findAllByKey(key));
  }

  public SortedMap<String, List<Translation>> getTranslationsGroupedByKey() {
    return translationRepository.findAll().stream()
        .collect(Collectors.groupingBy(Translation::getKey, TreeMap::new, Collectors.toList()));
  }

  public Map<String, Map<Locale, String>> getTranslationMaps() {
    var translations = translationRepository.findAll();

    return translations.stream()
        .collect(
            Collectors.groupingBy(
                Translation::getKey,
                Collectors.groupingBy(
                    Translation::getLocale,
                    Collectors.mapping(
                        Translation::getValue, Collectors.reducing("", (o1, o2) -> o2)))));
  }

  public Collection<Locale> getAvailableLocales() {
    return translationRepository.findDistinctLocale();
  }

  public Map<Locale, Map<Locale, String>> getTranslatedAvailableLocales() {
    Set<Locale> availableLocales = translationRepository.findDistinctLocale();
    return availableLocales.stream()
        .flatMap(l1 -> availableLocales.stream().map(l2 -> Pair.of(l1, l2)))
        .collect(
            Collectors.groupingBy(
                Pair::getFirst,
                Collectors.toMap(
                    Pair::getSecond, p -> p.getFirst().getDisplayLanguage(p.getSecond()))));
  }

  @NotNull
  private Collection<Pair<String, String>> parseKeyValueList(
      final String headKey, final Map<String, Object> translationObjects) {
    final var keyValueList = new LinkedList<Pair<String, String>>();

    translationObjects.forEach(
        (key, object) -> {
          if (object instanceof final String value) {
            final String translationKey = headKey + key;
            keyValueList.add(Pair.of(translationKey, value));
          } else if (object instanceof Map) {
            final String newHeadKey = headKey + key + ".";
            //noinspection unchecked
            final Map<String, Object> translationMap = (Map<String, Object>) object;
            keyValueList.addAll(parseKeyValueList(newHeadKey, translationMap));
          } else {
            throw new IllegalArgumentException(
                "Unable to parse translation map. Only Strings are allowed");
          }
        });

    return keyValueList;
  }
}
