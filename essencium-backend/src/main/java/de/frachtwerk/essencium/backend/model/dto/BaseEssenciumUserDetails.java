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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public interface BaseEssenciumUserDetails<ID extends Serializable> extends UserDetails {

  Set<? extends GrantedAuthority> getRoles();

  Set<? extends GrantedAuthority> getRights();

  String getFirstName();

  String getLastName();

  Locale getLocale();

  Map<String, Object> getAdditionalClaims();

  /**
   * Get additional claim by key.
   *
   * @param key the key of the additional claim
   * @return the additional claim value, or null if not found as Object
   */
  default Object getAdditionalClaimByKey(String key) {
    if (Objects.isNull(getAdditionalClaims())
        || getAdditionalClaims().isEmpty()
        || Objects.isNull(key)) return null;
    return getAdditionalClaims().get(key);
  }

  /**
   * Get additional claim by key and try to convert it to the specified class.
   *
   * <p>Currently supports conversion to Long, Integer, String, and Boolean. For other types it will
   * return the original object if it is assignable to the specified class.
   *
   * @param key the key of the additional claim
   * @param clazz the class to convert to (the type of the additional claim)
   * @return the additional claim value converted to the specified class, or null if not found
   */
  default <O> O getAdditionalClaimByKey(String key, Class<O> clazz) {
    Object object = getAdditionalClaims().get(key);
    if (object == null) {
      return null;
    } else if (clazz.isAssignableFrom(object.getClass())) {
      return clazz.cast(object);
    } else if (object instanceof Number number) {
      return clazz.cast(number.longValue());
    }
    return (O) object;
  }

  ID getId();
}
