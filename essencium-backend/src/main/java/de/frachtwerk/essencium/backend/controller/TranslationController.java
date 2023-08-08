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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.TranslationFileType;
import de.frachtwerk.essencium.backend.model.dto.TranslationDto;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.translation.TranslationFileService;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/translations")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.translation-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(
    name = "TranslationController",
    description = "Set of endpoints to manage language translations / i18n")
public class TranslationController {

  // TODO: Get rid of ?type parameter and do content negotiation via Accept headers instead

  private final TranslationService translationService;
  private final TranslationFileService translationFileService;

  @Autowired
  public TranslationController(
      final TranslationService translationService,
      final TranslationFileService translationFileService) {
    this.translationService = translationService;
    this.translationFileService = translationFileService;
  }

  @GetMapping("/{locale}")
  @Operation(
      description = "Download all translations for the given locale as a file in the given format")
  public ResponseEntity<Resource> getTranslationFile(
      @PathVariable @NotNull final Locale locale,
      @RequestParam(value = "type", defaultValue = "json") @NotNull
          final TranslationFileType fileType) {
    return translationFileService.getTranslationFile(locale, false, fileType);
  }

  @GetMapping(
      value = "/file",
      params = {"type"})
  @Operation(description = "Download a specific translation file in the given format")
  public ResponseEntity<Resource> getTranslationFile(
      @RequestParam(value = "type") final TranslationFileType fileType) {
    return translationFileService.getTranslationFile(false, fileType);
  }

  @PostMapping(consumes = {"application/json"})
  @Secured({BasicApplicationRight.Authority.TRANSLATION_UPDATE})
  @Operation(description = "Update an individual translation key-value pair")
  public Translation updateTranslation(@RequestBody @NotNull final TranslationDto translation) {
    return translationService.updateTranslation(
        new Translation(translation.getLocale(), translation.getKey(), translation.getValue()));
  }

  @PostMapping(consumes = {"multipart/form-data"})
  @Secured({BasicApplicationRight.Authority.TRANSLATION_UPDATE})
  @Operation(
      summary = "Upload a set of translations given by a file.",
      description =
          "File ending must correspond to one of the supported translation file format (json, xliff, properties)")
  public void updateTranslation(
      @Valid @RequestParam("file") @NotNull final MultipartFile translationFile,
      @Valid @RequestParam("locale") @NotNull final Locale locale) {
    translationFileService.updateTranslations(translationFile, locale);
  }

  @PutMapping("/{locale}/{key}")
  @Secured({BasicApplicationRight.Authority.TRANSLATION_UPDATE})
  @Operation(description = "Update an individual translation key-value pair")
  public Translation updateSingleTranslation(
      @PathVariable @NotNull final Locale locale,
      @PathVariable @NotNull final String key,
      @RequestBody @NotNull final String value) {
    return translationService.updateTranslation(new Translation(locale, key, value));
  }

  @PutMapping("/{locale}")
  @Secured({BasicApplicationRight.Authority.TRANSLATION_UPDATE})
  @Operation(description = "Update multiple translation key-value pairs at once")
  public Collection<Translation> updateTranslation(
      @PathVariable @NotNull final Locale locale,
      @RequestBody @NotNull final Map<String, Object> translationMap) {
    return translationService.updateTranslation(locale, translationMap);
  }

  @GetMapping
  @Operation(
      description =
          "Returns all translations grouped by their key, i.e. the returned result is a map of translation keys to a list of translations for every locale")
  public SortedMap<String, List<Translation>> getTranslationsGroupedByKey() {
    return translationService.getTranslationsGroupedByKey();
  }

  @GetMapping("/locales")
  @Operation(description = "List all available locales")
  public Map<Locale, Map<Locale, String>> getAvailableLocales() {
    return translationService.getTranslatedAvailableLocales();
  }

  @DeleteMapping("/delete/{key}")
  @Secured({BasicApplicationRight.Authority.TRANSLATION_DELETE})
  @Operation(description = "Delete an individual translation by its key")
  public void deleteTranslation(@PathVariable @NotNull final String key) {
    translationService.deleteTranslation(key);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(
        HttpMethod.GET,
        HttpMethod.HEAD,
        HttpMethod.POST,
        HttpMethod.PUT,
        HttpMethod.DELETE,
        HttpMethod.OPTIONS);
  }
}
