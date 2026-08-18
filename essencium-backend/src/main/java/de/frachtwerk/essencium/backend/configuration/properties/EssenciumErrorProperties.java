/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "essencium.error")
public class EssenciumErrorProperties {

  private static final String PROBE_CODE = "PROBE";

  @NotBlank private String urnPrefix = "urn:frachtwerk:error:";

  /** Fails startup rather than letting {@code URI.create} throw inside the exception handler. */
  @JsonIgnore
  @AssertTrue(message = "must form a valid URI together with an error code")
  public boolean isUrnPrefixParsable() {
    if (urnPrefix == null || urnPrefix.isBlank()) {
      return true; // reported by @NotBlank
    }

    try {
      URI.create(urnPrefix + PROBE_CODE);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
