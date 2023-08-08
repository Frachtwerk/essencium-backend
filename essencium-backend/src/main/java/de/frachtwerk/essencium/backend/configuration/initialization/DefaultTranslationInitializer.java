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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.service.translation.ResourceBundleParser;
import de.frachtwerk.essencium.backend.service.translation.TranslationFileUtil;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import de.frachtwerk.essencium.backend.util.ConfigurationUtils;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.data.util.Pair;

@Configuration
public class DefaultTranslationInitializer implements DataInitializer {

  private static final String DEFAULT_TRANSLATION_FILE_PATH_DE =
      "classpath:default_translation/translation-de.properties";

  private static final String DEFAULT_TRANSLATION_FILE_PATH_EN =
      "classpath:default_translation/translation-en.properties";

  private static final String DEFAULT_MAIL_TRANSLATION_FILE_PATH_DE =
      "classpath:default_translation/mailTranslation-de.properties";

  private static final String DEFAULT_MAIL_TRANSLATION_FILE_PATH_EN =
      "classpath:default_translation/mailTranslation-en.properties";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTranslationInitializer.class);

  private final TranslationService translationService;
  // TODO: use build in Properties
  private final ResourceBundleParser resourceBundleParser;
  private final ResourceLoader resourceLoader;

  @Autowired
  public DefaultTranslationInitializer(
      TranslationService translationService,
      ResourceBundleParser resourceBundleParser,
      ResourceLoader resourceLoader) {
    this.translationService = translationService;
    this.resourceBundleParser = resourceBundleParser;
    this.resourceLoader = resourceLoader;
  }

  protected Collection<String> getBasicTranslationFiles() {
    return Set.of(
        DEFAULT_TRANSLATION_FILE_PATH_DE,
        DEFAULT_TRANSLATION_FILE_PATH_EN,
        DEFAULT_MAIL_TRANSLATION_FILE_PATH_DE,
        DEFAULT_MAIL_TRANSLATION_FILE_PATH_EN);
  }

  protected Collection<String> getAdditionalApplicationTranslationFiles() {
    ClassPathResource resource = new ClassPathResource("translation");
    if (resource.exists()) {
      try {
        return Stream.of(
                ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:translation/*"))
            .map(this::getResourceURI)
            .map(URI::toString)
            .filter(s -> s.matches(".*-[a-z]{2}.properties"))
            .collect(Collectors.toSet());
      } catch (IOException e) {
        LOGGER.error("Failed to get additional translation files.", e);
      }
    }

    return Set.of();
  }

  @SneakyThrows
  private URI getResourceURI(Resource r) {
    return r.getURI();
  }

  @Override
  @Transactional
  public void run() {
    final AtomicInteger counter = new AtomicInteger();

    final var all =
        Stream.concat(
                getBasicTranslationFiles().stream(),
                getAdditionalApplicationTranslationFiles().stream())
            .map(
                f -> {
                  try {
                    return Pair.of(
                        TranslationFileUtil.parseLocaleFromFilename(f),
                        ConfigurationUtils.readResourceFileStream(f));
                  } catch (ParseException | IOException e) {
                    throw new RuntimeException(
                        String.format("Failed to read and parse translation file %s", f), e);
                  }
                })
            .flatMap(
                p -> {
                  final var parsedTranslations =
                      resourceBundleParser.parse(p.getSecond(), p.getFirst());
                  final var existingTranslations =
                      translationService.getTranslations(p.getFirst()).stream()
                          .collect(Collectors.toMap(Translation::getKey, Function.identity()));
                  return parsedTranslations.stream()
                      .filter(t -> !existingTranslations.containsKey(t.getKey()));
                })
            .peek(t -> counter.incrementAndGet())
            .collect(
                Collectors.toMap(
                    t -> String.format("%s__%s", t.getKey(), t.getLocale()),
                    Function.identity(),
                    (o1, o2) -> o2) // unique by keys, while latter ones take precedence
                )
            .values();

    translationService.updateTranslations(all);

    LOGGER.info("Initialized default translations ({} updated).", counter.get());
  }

  @Override
  public int order() {
    return 10;
  }
}
