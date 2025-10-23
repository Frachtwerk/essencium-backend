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

package de.frachtwerk.essencium.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.frachtwerk.essencium.backend.model.dto.BaseEssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.dto.RightGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.RoleGrantedAuthority;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public abstract class AbstractBaseUser<ID extends Serializable> extends AbstractBaseModel<ID>
    implements BaseEssenciumUserDetails<ID>, TitleConvention<ID> {

  public static final String USER_AUTH_SOURCE_LOCAL = "local";
  public static final String USER_AUTH_SOURCE_LDAP = "ldap";

  public static final Locale DEFAULT_LOCALE = Locale.GERMAN;
  public static final String PLACEHOLDER_FIRST_NAME = "Unknown";
  public static final String PLACEHOLDER_LAST_NAME = "Unknown";

  public static final String USER_ROLE_ATTRIBUTE = "roles";

  @Builder.Default private boolean enabled = true;

  @NotEmpty
  @Email
  @Column(unique = true, length = 150)
  private String email;

  @NotEmpty private String firstName;

  @NotEmpty private String lastName;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @JsonIgnore private String passwordResetToken;

  @Builder.Default @NotNull private Locale locale = DEFAULT_LOCALE;

  @NotNull
  @ManyToMany(fetch = FetchType.EAGER)
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  @ColumnDefault("0")
  @JsonIgnore
  private int failedLoginAttempts;

  @Builder.Default private boolean loginDisabled = false;

  private String source;

  public String getSource() {
    return Optional.ofNullable(source).orElse(USER_AUTH_SOURCE_LOCAL);
  }

  @Override
  @JsonIgnore
  public Collection<GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> rights =
        roles.stream()
            .map(Role::getRights)
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(HashSet::new));
    rights.addAll(roles.stream().map(Role::getRightFromRole).collect(Collectors.toSet()));
    return rights;
  }

  @JsonIgnore
  public Set<Right> getRights() {
    return roles.stream()
        .flatMap(role -> role.getRights().stream())
        .collect(Collectors.toCollection(HashSet::new));
  }

  @Override
  @JsonIgnore
  public String getUsername() {
    return email;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonExpired() {
    return enabled;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonLocked() {
    return !loginDisabled;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  @JsonIgnore
  public boolean isCredentialsNonExpired() {
    return enabled;
  }

  public boolean hasLocalAuthentication() {
    return getSource().equals(USER_AUTH_SOURCE_LOCAL);
  }

  public boolean hasAuthority(GrantedAuthority authority) {
    return getAuthorities().stream()
        .anyMatch(r -> r.getAuthority().equals(authority.getAuthority()));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;

    Class<?> oEffectiveClass =
        o instanceof HibernateProxy objectHibernateProxy
            ? objectHibernateProxy.getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy thisHibernateProxy
            ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();

    if (thisEffectiveClass != oEffectiveClass) return false;
    if (!(o instanceof AbstractBaseUser<?> other)) return false;
    return Objects.equals(getId(), other.getId()) && Objects.equals(getEmail(), other.getEmail());
  }

  @Override
  public String toString() {
    return "User " + getId() + "; " + email;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getEmail());
  }

  @Override
  public Map<String, Object> getAdditionalClaims() {
    return Map.of();
  }

  public EssenciumUserDetails<ID> toEssenciumUserDetails() {
    return EssenciumUserDetails.<ID>builder()
        .id(this.getId())
        .username(this.getUsername())
        .firstName(this.getFirstName())
        .lastName(this.getLastName())
        .locale(this.getLocale().toLanguageTag())
        .roles(
            this.getRoles().stream()
                .map(role -> new RoleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet()))
        .rights(
            this.getAuthorities().stream()
                .map(authority -> new RightGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toSet()))
        .additionalClaims(this.getAdditionalClaims())
        .build();
  }
}
