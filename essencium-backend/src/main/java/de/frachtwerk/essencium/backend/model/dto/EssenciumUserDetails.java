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

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuperBuilder
@Data
@Getter
@AllArgsConstructor
public class EssenciumUserDetails<ID extends Serializable> implements UserDetails {
  private final ID id;
  private final String username;
  private final String firstName;
  private final String lastName;
  private final String locale;
  private final Set<? extends GrantedAuthority> roles;
  private final Set<? extends GrantedAuthority> rights;
  private final Map<String, Object> additionalClaims;

  public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Stream.concat(
            roles.stream().map(role -> new RoleGrantedAuthority(role.getAuthority())),
            rights.stream().map(right -> new RightGrantedAuthority(right.getAuthority())))
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    return "";
  }

  public Locale getLocale() {
    return Objects.requireNonNullElse(Locale.forLanguageTag(locale), DEFAULT_LOCALE);
  }

  public ID getId() {
    return switch (id) {
      case Long l -> (ID) l;
      case Integer i -> (ID) Long.valueOf(i.longValue());
      default -> id;
    };
  }

  public Object getAdditionalClaimByKey(String key) {
    return additionalClaims.get(key);
  }

  public <O> O getAdditionalClaimByKey(String key, Class<O> clazz) {
    Object object = getAdditionalClaims().get(key);
    if (object != null && clazz.isAssignableFrom(object.getClass())) {
      return clazz.cast(object);
    } else if (object == null) {
      return null;
    }

    if (clazz == Long.class) {
      switch (object) {
        case Number number -> {
          return clazz.cast(number.longValue());
        }
        case String str -> {
          try {
            return clazz.cast(Long.valueOf(str));
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert String to Long: " + str, e);
          }
        }
        case Boolean bool -> {
          return clazz.cast(bool ? 1L : 0L);
        }
        default -> {}
      }
    } else if (clazz == Integer.class) {
      switch (object) {
        case Number number -> {
          return clazz.cast(number.intValue());
        }
        case String str -> {
          try {
            return clazz.cast(Integer.valueOf(str));
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert String to Integer: " + str, e);
          }
        }
        case Boolean bool -> {
          return clazz.cast(bool ? 1 : 0);
        }
        default -> {}
      }
    } else if (clazz == String.class) {
      return clazz.cast(object.toString());
    } else if (clazz == Boolean.class) {
      switch (object) {
        case Boolean bool -> {
          return clazz.cast(bool);
        }
        case String str -> {
          return clazz.cast(Boolean.valueOf(str));
        }
        case Number number -> {
          return clazz.cast(number.intValue() != 0);
        }
        default -> {}
      }
    } else {
      return clazz.cast(object);
    }
    return (O) object;
  }
}
