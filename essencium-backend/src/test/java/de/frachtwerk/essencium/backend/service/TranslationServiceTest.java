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

package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TranslationServiceTest {

  private final TranslationRepository translationRepositoryMock = mock(TranslationRepository.class);

  private final TranslationService testSubject = new TranslationService(translationRepositoryMock);

  private final Locale testLocale = mock(Locale.class);

  private final List<Translation> testTranslations =
      List.of(
          new Translation(testLocale, "test-entry", "Testeintrag"),
          new Translation(testLocale, "other", "Was anderes"),
          new Translation(testLocale, "BANANARAMA", "Platzhalter für unkreative Testeingaben"),
          new Translation(testLocale, "users.name.first", "Vorname"),
          new Translation(testLocale, "users.name.last", "Nachname"),
          new Translation(testLocale, "users.address.street", "Straße"),
          new Translation(testLocale, "users.address.number", "Hausnummer"),
          new Translation(testLocale, "users.address.zip", "Postleitzahl"),
          new Translation(testLocale, "users.address.place", "Ort"),
          new Translation(testLocale, "users.address.country", "Land"),
          new Translation(testLocale, "users.mail", "eMail"),
          new Translation(testLocale, "users.phone", "Telefon"));

  // TODO: More tests

  @Test
  void getTranslationsGroupedByKey() {
    var testTranslations =
        Lists.newArrayList(
            new Translation(Locale.CANADA, "key.test", "test"),
            new Translation(Locale.FRANCE, "key.test", "contrôle"),
            new Translation(Locale.GERMAN, "key.test", "Test"),
            new Translation(Locale.ENGLISH, "key.test", "test"));

    var acceptanceTranslations =
        Lists.newArrayList(
            new Translation(Locale.CANADA, "key.acceptance", "acceptance"),
            new Translation(Locale.FRANCE, "key.acceptance", "réception"),
            new Translation(Locale.GERMAN, "key.acceptance", "Abnahme"),
            new Translation(Locale.ENGLISH, "key.acceptance", "acceptance"));

    var translations =
        Stream.concat(testTranslations.stream(), acceptanceTranslations.stream())
            .collect(Collectors.toList());

    when(translationRepositoryMock.findAll()).thenReturn(translations);

    var retVal = testSubject.getTranslationsGroupedByKey();

    assertThat(retVal).containsOnlyKeys("key.test", "key.acceptance");
    Assertions.assertThat(retVal.get("key.test"))
        .containsExactlyInAnyOrderElementsOf(testTranslations);
    Assertions.assertThat(retVal.get("key.acceptance"))
        .containsExactlyInAnyOrderElementsOf(acceptanceTranslations);
  }

  @Test
  void getAvailableLocales() {
    var testTranslations =
        Lists.newArrayList(
            new Translation(Locale.CANADA, "key.test", "test"),
            new Translation(Locale.FRANCE, "key.test", "contrôle"),
            new Translation(Locale.GERMAN, "key.test", "Test"),
            new Translation(Locale.ENGLISH, "key.test", "test"));

    var acceptanceTranslations =
        Lists.newArrayList(
            new Translation(Locale.CANADA, "key.acceptance", "acceptance"),
            new Translation(Locale.FRANCE, "key.acceptance", "réception"),
            new Translation(Locale.GERMAN, "key.acceptance", "Abnahme"),
            new Translation(Locale.ENGLISH, "key.acceptance", "acceptance"));

    var locales =
        Stream.concat(testTranslations.stream(), acceptanceTranslations.stream())
            .map(Translation::getLocale)
            .collect(Collectors.toSet());

    when(translationRepositoryMock.findDistinctLocale()).thenReturn(locales);

    var retVal = testSubject.getAvailableLocales();

    assertThat(retVal)
        .containsExactlyInAnyOrder(Locale.CANADA, Locale.FRANCE, Locale.GERMAN, Locale.ENGLISH);
  }

  @Test
  void getTranslatedAvailableLocales() {
    // Support both: lang codes and lang+count codes
    var testLocales = Set.of(Locale.GERMANY, Locale.US);

    when(translationRepositoryMock.findDistinctLocale()).thenReturn(testLocales);

    var retVal = testSubject.getTranslatedAvailableLocales();

    assertThat(retVal).hasSize(2).containsKeys(Locale.GERMANY, Locale.US);
    assertThat(retVal.get(Locale.GERMANY)).containsKeys(Locale.GERMANY, Locale.US);
    assertThat(retVal.get(Locale.US)).containsKeys(Locale.GERMANY, Locale.US);
    assertThat(retVal.get(Locale.GERMANY).get(Locale.GERMANY)).isEqualTo("Deutsch");
    assertThat(retVal.get(Locale.GERMANY).get(Locale.US)).isEqualTo("German");
    assertThat(retVal.get(Locale.US).get(Locale.GERMANY)).isEqualTo("Englisch");
    assertThat(retVal.get(Locale.US).get(Locale.US)).isEqualTo("English");
  }

  @Test
  void deleteTranslation() {
    var testKey = "test.key";
    when(translationRepositoryMock.findAllByKey(testKey))
        .thenReturn(List.of(Mockito.mock(Translation.class)));
    doNothing().when(translationRepositoryMock).deleteAll();

    testSubject.deleteTranslation(testKey);

    verify(translationRepositoryMock).deleteAll(translationRepositoryMock.findAllByKey(testKey));
  }

  @Test
  void testTranslate() {
    final var testTranslation = Pair.of("some.key", "Some Value");
    when(translationRepositoryMock.findByKeyAndLocale(testTranslation.getKey(), Locale.GERMANY))
        .thenReturn(
            Optional.of(
                Translation.builder()
                    .key(testTranslation.getKey())
                    .value(testTranslation.getValue())
                    .locale(Locale.GERMANY)
                    .build()));

    final var result = testSubject.translate(testTranslation.getKey(), Locale.GERMANY);
    assertThat(result).isPresent();
    assertThat(result).contains("Some Value");
  }

  @Test
  void testTranslateVariant() {
    final var testTranslation = Pair.of("foo.car", "Car | Cars");
    when(translationRepositoryMock.findByKeyAndLocale(testTranslation.getKey(), Locale.GERMANY))
        .thenReturn(
            Optional.of(
                Translation.builder()
                    .key(testTranslation.getKey())
                    .value(testTranslation.getValue())
                    .locale(Locale.GERMANY)
                    .build()));

    final var result1 = testSubject.translate(testTranslation.getKey(), Locale.GERMANY);
    assertThat(result1).isPresent();
    assertThat(result1).contains("Car");

    final var result2 = testSubject.translateVariant(testTranslation.getKey(), Locale.GERMANY, 1);
    assertThat(result2).isPresent();
    assertThat(result2).contains("Cars");

    final var result3 = testSubject.translateVariant(testTranslation.getKey(), Locale.GERMANY, 2);
    assertThat(result3.isEmpty()).isTrue();
  }
}
