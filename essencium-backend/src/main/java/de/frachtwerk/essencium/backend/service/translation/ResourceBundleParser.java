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

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ResourceBundleParser implements TranslationFileParser {

  private static final String SEPARATOR = "=";
  private static final String COMMENT_START = "#";

  @Override
  public Collection<Translation> parse(
      @NotNull final InputStream fileStream, @NotNull final Locale targetLocale) {
    var translations = new LinkedList<Translation>();

    try (var fileReader = new BufferedReader(new InputStreamReader(fileStream))) {
      while (fileReader.ready()) {
        var currentLine = fileReader.readLine();
        if (!currentLine.isBlank() && !currentLine.startsWith(COMMENT_START)) {
          translations.add(parseSingleLine(currentLine, targetLocale));
        }
      }
    } catch (IOException e) {
      throw new TranslationFileException(e);
    }

    return translations;
  }

  @NotNull
  private Translation parseSingleLine(
      @NotNull final String singleLine, @NotNull final Locale locale) {
    var splitString = singleLine.split(SEPARATOR, 2);
    if (splitString.length != 2) {
      throw new TranslationFileException("Unable to parse line: " + singleLine);
    }

    return new Translation(locale, splitString[0], splitString[1]);
  }
}
