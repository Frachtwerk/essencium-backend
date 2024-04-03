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

package de.frachtwerk.essencium.backend.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;

@Data
@Entity
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Role implements GrantedAuthority {

  @NotNull @Id private String name;

  private String description;

  private boolean isProtected;

  private boolean isDefaultRole;

  private boolean isSystemRole;

  @ManyToMany(fetch = FetchType.EAGER)
  private Set<Right> rights;

  @JsonGetter(value = "editable")
  public boolean isEditable() {
    return !isProtected; // for backwards compatibility in existing projects
  }

  @Override
  @JsonIgnore
  public String getAuthority() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Role role)) return false;
    return getName().equals(role.getName());
  }

  @JsonIgnore
  public Right getRightFromRole() {
    return Right.builder().authority(this.getName()).description(this.getDescription()).build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName());
  }
}
