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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;

@Data
@Entity
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Role implements GrantedAuthority, Cloneable {

  @Serial private static final long serialVersionUID = 2405172041950250807L;

  @NotNull @Id private String name;

  private String description;

  private boolean isProtected;

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

  public boolean equalsDto(RoleDto dto) {
    final Set<String> localRights =
        getRights().stream().map(Right::getAuthority).collect(Collectors.toSet());
    final Set<String> otherRights = dto.getRights();

    return Objects.equals(getName(), dto.getName())
        && Objects.equals(getDescription(), dto.getDescription())
        && otherRights.containsAll(localRights)
        && localRights.containsAll(otherRights);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Role role)) return false;
    return isProtected() == role.isProtected()
        && getName().equals(role.getName())
        && Objects.equals(getDescription(), role.getDescription())
        && Objects.equals(getRights(), role.getRights());
  }

  @JsonIgnore
  public Right getRightFromRole() {
    return Right.builder().authority(this.getName()).description(this.getDescription()).build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getDescription(), isProtected(), getRights());
  }

  @Override
  public Role clone() {
    try {
      Role clone = (Role) super.clone();
      clone.setName(getName());
      clone.setDescription(getDescription());
      clone.setProtected(isProtected());
      clone.setRights(getRights());
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
