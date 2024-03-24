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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"linkedUser", "description"})})
public class ApiTokenUser implements UserDetails {

  @Transient public static final String USER_SPLITTER = ":";

  @Id @UuidGenerator private UUID id;

  private String linkedUser;

  private String description;

  @ManyToMany(cascade = CascadeType.ALL)
  @Builder.Default
  private Set<Right> rights = new HashSet<>();

  @CreatedDate private LocalDateTime createdAt;

  private LocalDate validUntil;

  private boolean disabled;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return rights;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return linkedUser + USER_SPLITTER + id;
  }

  @Override
  public boolean isAccountNonExpired() {
    return (LocalDate.now().isBefore(validUntil) || LocalDate.now().isEqual(validUntil));
  }

  @Override
  public boolean isAccountNonLocked() {
    return !disabled;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return !disabled;
  }

  @Override
  public boolean isEnabled() {
    return isAccountNonExpired() && isAccountNonLocked();
  }
}
