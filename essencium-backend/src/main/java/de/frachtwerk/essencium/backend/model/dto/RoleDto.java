/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.model.dto;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
  @NotBlank private String name;

  private String description;

  private boolean isProtected;

  private boolean isDefaultRole;

  @Builder.Default private Set<Object> rights = new HashSet<>();

  public Role toRole() {
    HashSet<Right> convertedRights = new HashSet<>();
    if (!rights.isEmpty()) {
      // patterns in switch statements are a preview feature and are disabled by default.
      if (rights.iterator().next() instanceof Right) {
        convertedRights.addAll(
            rights.stream().map(o -> (Right) o).collect(Collectors.toCollection(HashSet::new)));
      } else if (rights.iterator().next() instanceof Map<?, ?>) {
        convertedRights.addAll(
            rights.stream()
                .map(o -> (Map<String, String>) o)
                .map(
                    map ->
                        Right.builder()
                            .authority(map.get("authority"))
                            .description(map.get("description"))
                            .build())
                .collect(Collectors.toCollection(HashSet::new)));
      } else if (rights.iterator().next() instanceof String) {
        convertedRights.addAll(
            rights.stream()
                .map(o -> Right.builder().authority((String) o).build())
                .collect(Collectors.toCollection(HashSet::new)));
      }
    }
    return Role.builder()
        .name(name)
        .description(description)
        .isProtected(isProtected)
        .isDefaultRole(isDefaultRole)
        .rights(convertedRights)
        .build();
  }
}
