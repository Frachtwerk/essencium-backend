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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.service.translation.ResourceBundleParser;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ResourceLoader;

class DefaultTranslationInitializerTest {

  private final TranslationService translationServiceMock = mock(TranslationService.class);
  private final ResourceBundleParser resourceBundleParserMock = mock(ResourceBundleParser.class);
  private final ResourceLoader resourceLoaderMock = mock(ResourceLoader.class);

  @Captor private ArgumentCaptor<Collection<Translation>> translationListCaptor;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testNoTranslationsPersistent() {
    final var sut =
        new DefaultTranslationInitializer(
            translationServiceMock, resourceBundleParserMock, resourceLoaderMock);

    when(translationServiceMock.getTranslations()).thenReturn(Collections.emptyList());
    when(translationServiceMock.getTranslations(any())).thenReturn(Collections.emptyList());

    DefaultTranslationInitializer sut1 = Mockito.spy(sut);
    Mockito.doReturn(Collections.emptySet()).when(sut1).getAdditionalApplicationTranslationFiles();

    final var germanTestTranslations =
        List.of(
            new Translation(Locale.GERMAN, "a", "a-de"),
            new Translation(Locale.GERMAN, "b", "b-de"),
            new Translation(Locale.GERMAN, "c", "c-de"));

    final var englishTestTranslations =
        List.of(
            new Translation(Locale.ENGLISH, "a", "a-en"),
            new Translation(Locale.ENGLISH, "b", "b-en"),
            new Translation(Locale.ENGLISH, "c", "c-en"));

    final var germanTestMailTranslations = List.of(new Translation(Locale.GERMAN, "d", "d-de"));

    final var englishTestMailTranslations = List.of(new Translation(Locale.ENGLISH, "d", "d-en"));

    final var expectedTranslations = new LinkedList<>(germanTestTranslations);
    expectedTranslations.addAll(englishTestTranslations);
    expectedTranslations.addAll(germanTestMailTranslations);
    expectedTranslations.addAll(englishTestMailTranslations);

    when(resourceBundleParserMock.parse(any(), eq(Locale.GERMAN)))
        .thenReturn(germanTestTranslations)
        .thenReturn(germanTestMailTranslations);

    when(resourceBundleParserMock.parse(any(), eq(Locale.ENGLISH)))
        .thenReturn(englishTestTranslations)
        .thenReturn(englishTestMailTranslations);

    sut1.run();

    verify(resourceBundleParserMock, times(2)).parse(any(), eq(Locale.GERMAN));
    verify(resourceBundleParserMock, times(2)).parse(any(), eq(Locale.ENGLISH));
    verify(translationServiceMock, times(4)).getTranslations(any(Locale.class));
    verify(translationServiceMock).updateTranslations(translationListCaptor.capture());

    assertThat(translationListCaptor.getValue())
        .containsExactlyInAnyOrderElementsOf(expectedTranslations);

    verifyNoMoreInteractions(resourceBundleParserMock);
    verifyNoMoreInteractions(translationServiceMock);
  }

  @Test
  void testNotOverwritePresentTranslations() {
    final var sut =
        new DefaultTranslationInitializer(
            translationServiceMock, resourceBundleParserMock, resourceLoaderMock);

    final Translation testExisting1 = new Translation(Locale.GERMAN, "a", "a-modified");
    final Translation testNew1 = new Translation(Locale.GERMAN, "a", "a");
    final Translation testNew2 = new Translation(Locale.GERMAN, "b", "b");

    when(translationServiceMock.getTranslations(Locale.GERMAN)).thenReturn(List.of(testExisting1));
    when(translationServiceMock.getTranslations(Locale.ENGLISH)).thenReturn(List.of());

    DefaultTranslationInitializer sut1 = Mockito.spy(sut);
    Mockito.doReturn(Collections.emptySet()).when(sut1).getAdditionalApplicationTranslationFiles();

    final List<Translation> germanTestTranslations = List.of(testNew1, testNew2);
    final List<Translation> englishTestTranslations = List.of();

    final List<Translation> expectedTranslations = List.of(testNew2);

    when(resourceBundleParserMock.parse(any(), eq(Locale.GERMAN)))
        .thenReturn(germanTestTranslations)
        .thenReturn(new ArrayList<>());

    when(resourceBundleParserMock.parse(any(), eq(Locale.ENGLISH)))
        .thenReturn(englishTestTranslations)
        .thenReturn(new ArrayList<>());

    sut1.run();

    verify(translationServiceMock).updateTranslations(translationListCaptor.capture());
    assertThat(translationListCaptor.getValue())
        .containsExactlyInAnyOrderElementsOf(expectedTranslations);
  }

  @Test
  void testAppTranslationsTakePrecedence() {
    final var sut =
        new DefaultTranslationInitializer(
            translationServiceMock, resourceBundleParserMock, resourceLoaderMock);

    final Translation testNew1 = new Translation(Locale.GERMAN, "a", "a-starter");
    final Translation testNew2 = new Translation(Locale.GERMAN, "a", "a-myapp");

    when(translationServiceMock.getTranslations(Locale.GERMAN)).thenReturn(List.of());
    when(translationServiceMock.getTranslations(Locale.ENGLISH)).thenReturn(List.of());

    DefaultTranslationInitializer sut1 = Mockito.spy(sut);
    Mockito.doReturn(Collections.emptySet()).when(sut1).getAdditionalApplicationTranslationFiles();

    final List<Translation> germanTestTranslations = List.of(testNew1, testNew2);
    final List<Translation> englishTestTranslations = List.of();

    final List<Translation> expectedTranslations = List.of(testNew2);

    when(resourceBundleParserMock.parse(any(), eq(Locale.GERMAN)))
        .thenReturn(germanTestTranslations)
        .thenReturn(new ArrayList<>());

    when(resourceBundleParserMock.parse(any(), eq(Locale.ENGLISH)))
        .thenReturn(englishTestTranslations)
        .thenReturn(new ArrayList<>());

    sut1.run();

    verify(translationServiceMock).updateTranslations(translationListCaptor.capture());
    assertThat(translationListCaptor.getValue())
        .containsExactlyInAnyOrderElementsOf(expectedTranslations);
  }
}
