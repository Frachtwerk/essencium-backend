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
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.reader.XLIFFReaderException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class XliffParser implements TranslationFileParser {

  @Override
  public Collection<Translation> parse(final InputStream fileStream, final Locale targetLocale) {
    return parse(fileStream).get(Locale.forLanguageTag(targetLocale.getLanguage()));
  }

  public Map<Locale, Collection<Translation>> parse(final InputStream fileStream) {
    var xliffReader = new XLIFFReader();
    xliffReader.open(fileStream);

    var xliffParsingUnit = new XliffParsingUnit(xliffReader);
    xliffParsingUnit.parse();

    return xliffParsingUnit.translationMap;
  }

  private static class XliffParsingUnit {
    private final XLIFFReader xliffReader;

    Locale sourceLocale;
    @Nullable Locale targetLocale;

    final Map<Locale, Collection<Translation>> translationMap = new HashMap<>();

    Collection<Translation> sourceTranslations = new LinkedList<>();
    Collection<Translation> targetTranslations = new LinkedList<>();

    private XliffParsingUnit(@NotNull final XLIFFReader xliffReader) {
      this.xliffReader = xliffReader;
    }

    void parse() {

      try (xliffReader) {
        while (xliffReader.hasNext()) {
          Event event = xliffReader.next();
          // Do something: here print the source content

          switch (event.getType()) {
            case START_XLIFF:
              handleStartXliff(event.getStartXliffData());
              break;
            case START_FILE:
              handleStartFile(event.getStartFileData());
              break;
            case TEXT_UNIT:
              handleUnit(event.getUnit());
              break;
            case START_DOCUMENT:
            case END_DOCUMENT:
            case END_XLIFF:
            case MID_FILE:
            case END_FILE:
            case SKELETON:
            case START_GROUP:
            case END_GROUP:
            case INSIGNIFICANT_PART:
              break;
          }
        }

      } catch (XLIFFReaderException xliffException) {
        throw new TranslationFileException("Unable to parse xliff-file", xliffException);
      }
    }

    private void handleStartFile(final StartFileData startFileData) {
      if (targetLocale == null) {
        targetLocale = Locale.forLanguageTag(startFileData.getId());
      }
    }

    private void handleUnit(@NotNull final Unit unit) {
      Assert.state(targetLocale != null, "Unit can not be parsed without valid target language!");
      var unitTag = unit.getId();

      unit.getSegments()
          .forEach(
              segment -> {
                final var segmentTag = segment.getId();
                final var key = createKey(unitTag, segmentTag);

                final var sourceTranslationString = segment.getSource().getPlainText();
                final Translation sourceTranslation;

                var target = segment.getTarget();
                if (target != null) {
                  sourceTranslation = new Translation(sourceLocale, key, sourceTranslationString);
                  var targetTranslationString = segment.getTarget().getPlainText();
                  var targetTranslation =
                      new Translation(targetLocale, key, targetTranslationString);

                  putToTranslationMap(targetTranslation);
                } else {
                  sourceTranslation = new Translation(targetLocale, key, sourceTranslationString);
                }
                putToTranslationMap(sourceTranslation);
              });
    }

    private void putToTranslationMap(@NotNull final Translation translation) {
      var translationLocale = translation.getLocale();
      if (!translationMap.containsKey(translationLocale)) {
        translationMap.put(translationLocale, new LinkedList<>());
      }

      translationMap.get(translationLocale).add(translation);
    }

    @NotNull
    private String createKey(@Nullable final String unitTag, @Nullable final String segmentTag) {
      var key = unitTag == null ? "" : unitTag.isBlank() ? "" : unitTag;
      key += segmentTag == null ? "" : segmentTag.isBlank() ? "" : "." + segmentTag;

      return key;
    }

    private void handleStartXliff(@NotNull final StartXliffData startXliffData) {
      var srcLanguageString = startXliffData.getSourceLanguage();
      var targetLanguageString = startXliffData.getTargetLanguage();

      sourceLocale = Locale.forLanguageTag(srcLanguageString);
      targetLocale =
          targetLanguageString == null ? null : Locale.forLanguageTag(targetLanguageString);
    }
  }
}
