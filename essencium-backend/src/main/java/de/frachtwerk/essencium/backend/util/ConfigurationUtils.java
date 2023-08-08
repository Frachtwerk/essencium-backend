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

package de.frachtwerk.essencium.backend.util;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.ResourceUtils;

public final class ConfigurationUtils {
  private ConfigurationUtils() {}

  public static InputStream readResourceFileStream(@NotNull final String resourcePath)
      throws IOException {
    return ResourceUtils.getURL(resourcePath).openStream();
  }

  public static byte[] readResourceFileBytes(@NotNull final String resourcePath)
      throws IOException {
    try (var stream = readResourceFileStream(resourcePath)) {
      return stream.readAllBytes();
    }
  }
}
