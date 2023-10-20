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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.ReadOnlyProperty;

@Data
@Entity
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionToken {

  @Id @UuidGenerator private UUID id;

  @NotNull @ReadOnlyProperty @ToString.Exclude private SecretKey key;

  @NotNull @ReadOnlyProperty private String username;

  @Enumerated(EnumType.STRING)
  private SessionTokenType type;

  private Date issuedAt;

  private Date expiration;

  @ManyToOne private SessionToken parentToken;

  private String userAgent;

  @OneToMany(mappedBy = "parentToken", cascade = CascadeType.ALL)
  private List<SessionToken> accessTokens;

  public LocalDateTime getLastUsed() {
    if (accessTokens.isEmpty()) return null;
    return accessTokens.stream()
        .max(Comparator.comparing(SessionToken::getIssuedAt))
        .get()
        .getLastUsed();
  }
}
