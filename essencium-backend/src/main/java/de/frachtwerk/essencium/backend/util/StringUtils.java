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

package de.frachtwerk.essencium.backend.util;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.regex.Pattern;

public final class StringUtils {

  private StringUtils() {}

  @NotNull
  public static String getFileType(@NotNull final String path) {
    var typeSeparatorIndex = path.lastIndexOf(".");

    if (typeSeparatorIndex > 0) {
      return path.substring(typeSeparatorIndex + 1);
    } else {
      throw new IllegalArgumentException("Passed path has no file type ending: [" + path + "]");
    }
  }

  public static boolean isValidEmailAddress(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    // Deviation from RFC 5322: Special characters like "(),:;<>@[\] are not allowed. Quotation
    // marks are also not allowed. This prevents the use of quoted characters
    // (https://www.rfc-editor.org/rfc/rfc5322#section-3.2.1). Email addresses like
    // "user@something"@example.com are therefore not possible.
    //
    // Length of local part: 1-64 characters
    // Length of domain part: 2-255 characters
    // https://datatracker.ietf.org/doc/html/rfc1035#section-2.3.4
    String emailRegex =
        "^(?=^[^@ ]{1,64}@[^@ ]{4,255}$)(?![.;])(?![^@]*@.*\\.\\..*)(?!.*\\.$)" // Pattern
            + "[.a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]*@[^-.][.A-Za-z0-9_-]*\\.[A-Za-z]{2,}$"; // Characters
    return Pattern.matches(emailRegex, email);
  }

  public static String[] parseFirstLastName(@Nullable String combinedName) {
    final var parsedName = new String[2];

    if (combinedName == null) {
      return parsedName;
    }

    final var matches = combinedName.split(" ", 2);
    System.arraycopy(matches, 0, parsedName, 0, matches.length);

    return parsedName;
  }
}
