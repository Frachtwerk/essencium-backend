package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.TranslationFileType;
import de.frachtwerk.essencium.backend.model.dto.TranslationDto;
import de.frachtwerk.essencium.backend.service.translation.TranslationFileService;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

class TranslationControllerTestMeTest {
  @Mock TranslationService translationService;
  @Mock TranslationFileService translationFileService;
  @InjectMocks TranslationController translationController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetTranslationFile() {
    when(translationFileService.getTranslationFile(
            any(Locale.class), anyBoolean(), any(TranslationFileType.class)))
        .thenReturn(new ResponseEntity<Resource>(null, null, 0));

    ResponseEntity<Resource> result =
        translationController.getTranslationFile(
            new Locale("language", "country", "variant"), TranslationFileType.JSON);
    Assertions.assertEquals(new ResponseEntity<Resource>(null, null, 0), result);
  }

  @Test
  void testGetTranslationFile2() {
    when(translationFileService.getTranslationFile(anyBoolean(), any(TranslationFileType.class)))
        .thenReturn(new ResponseEntity<Resource>(null, null, 0));

    ResponseEntity<Resource> result =
        translationController.getTranslationFile(TranslationFileType.JSON);
    Assertions.assertEquals(new ResponseEntity<Resource>(null, null, 0), result);
  }

  @Test
  void testUpdateTranslation() {
    when(translationService.updateTranslation(any(Translation.class)))
        .thenReturn(new Translation(new Locale("language", "country", "variant"), "key", "value"));

    Translation result =
        translationController.updateTranslation(
            new TranslationDto(new Locale("language", "country", "variant"), "key", "value"));
    Assertions.assertEquals(
        new Translation(new Locale("language", "country", "variant"), "key", "value"), result);
  }

  @Test
  void testUpdateTranslation2() {
    translationController.updateTranslation(null, new Locale("language", "country", "variant"));
    verify(translationFileService).updateTranslations(any(MultipartFile.class), any(Locale.class));
  }

  @Test
  void testUpdateSingleTranslation() {
    when(translationService.updateTranslation(any(Translation.class)))
        .thenReturn(new Translation(new Locale("language", "country", "variant"), "key", "value"));

    Translation result =
        translationController.updateSingleTranslation(
            new Locale("language", "country", "variant"), "key", "value");
    Assertions.assertEquals(
        new Translation(new Locale("language", "country", "variant"), "key", "value"), result);
  }

  @Test
  void testUpdateTranslation3() {
    when(translationService.updateTranslation(any(Locale.class), any(Map.class)))
        .thenReturn(
            List.of(new Translation(new Locale("language", "country", "variant"), "key", "value")));

    Collection<Translation> result =
        translationController.updateTranslation(
            new Locale("language", "country", "variant"),
            Map.of("translationMap", "translationMap"));
    Assertions.assertEquals(
        List.of(new Translation(new Locale("language", "country", "variant"), "key", "value")),
        result);
  }

  @Test
  void testGetTranslationsGroupedByKey() {
    when(translationService.getTranslationsGroupedByKey()).thenReturn(null);

    SortedMap<String, List<Translation>> result =
        translationController.getTranslationsGroupedByKey();
    Assertions.assertEquals(null, result);
  }

  @Test
  void testGetAvailableLocales() {
    when(translationService.getTranslatedAvailableLocales())
        .thenReturn(
            Map.of(
                new Locale("language", "country", "variant"),
                Map.of(
                    new Locale("language", "country", "variant"),
                    "getTranslatedAvailableLocalesResponse")));

    Map<Locale, Map<Locale, String>> result = translationController.getAvailableLocales();
    Assertions.assertEquals(
        Map.of(
            new Locale("language", "country", "variant"),
            Map.of(new Locale("language", "country", "variant"), "replaceMeWithExpectedResult")),
        result);
  }

  @Test
  void testDeleteTranslation() {
    translationController.deleteTranslation("key");
    verify(translationService).deleteTranslation(anyString());
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = translationController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = translationController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
