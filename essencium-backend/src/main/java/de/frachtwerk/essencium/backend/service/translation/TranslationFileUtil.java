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

import de.frachtwerk.essencium.backend.model.Translation;
import jakarta.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationFileUtil {

  private static final Pattern LOCALE_FILENAME_REGEX =
      Pattern.compile("^.*-([a-z]{2}).properties$");

  @NotNull
  public static Map<Locale, Collection<Translation>> groupByLocale(
      final Collection<Translation> translations) {
    final var groupedLocaleMap = new HashMap<Locale, Collection<Translation>>();

    translations.forEach(
        translation -> {
          groupedLocaleMap.putIfAbsent(translation.getLocale(), new LinkedList<>());
          groupedLocaleMap.get(translation.getLocale()).add(translation);
        });

    return groupedLocaleMap;
  }

  public static Locale parseLocaleFromFilename(String fileName) throws ParseException {
    final Matcher matcher = LOCALE_FILENAME_REGEX.matcher(fileName);
    if (!matcher.matches() || matcher.groupCount() != 1) {
      throw new ParseException(
          String.format("failed to parse locale from file name '%s'", fileName), 0);
    }
    return Locale.forLanguageTag(matcher.group(1).replace("_", "-"));
  }
}
