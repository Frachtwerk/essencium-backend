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

package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class TokenRepresentation {

  private UUID id;

  private SessionTokenType type;

  private String username;

  private Date issuedAt;

  private Date expiration;

  private String userAgent;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastUsed;

  public static TokenRepresentation from(SessionToken entity) {
    return TokenRepresentation.builder()
        .id(entity.getId())
        .type(entity.getType())
        .username(entity.getUsername())
        .issuedAt(entity.getIssuedAt())
        .expiration(entity.getExpiration())
        .userAgent(entity.getUserAgent())
        .lastUsed(
            Optional.ofNullable(entity.getLastUsed())
                .map(Date::toInstant)
                .map(instant -> instant.atZone(ZoneOffset.UTC))
                .map(ZonedDateTime::toLocalDateTime)
                .orElse(null))
        .build();
  }
}
