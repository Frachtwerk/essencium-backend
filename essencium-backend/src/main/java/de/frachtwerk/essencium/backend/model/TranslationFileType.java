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

package de.frachtwerk.essencium.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TranslationFileType {
  JSON(Set.of("json")),
  PROPERTIES(Set.of("properties")),
  RESOURCEBUNDLE(Set.of("resourcebundle", "resource-bundle")),
  XLIFF(Set.of("xliff", "xlf")),
  UNKNOWN(Set.of());

  private final Set<String> aliases;

  TranslationFileType(Set<String> aliases) {
    this.aliases = aliases.stream().map(String::toLowerCase).collect(Collectors.toSet());
  }

  @JsonValue
  public String getKey() {
    return this.name().toLowerCase();
  }

  public boolean hasAlias(String alias) {
    return this.aliases.contains(alias.toLowerCase());
  }

  @JsonCreator
  public static TranslationFileType from(String name) {
    return Arrays.stream(TranslationFileType.values())
        .filter(t -> t.hasAlias(name))
        .findFirst()
        .orElse(UNKNOWN);
  }
}
