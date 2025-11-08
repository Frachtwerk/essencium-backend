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

package de.frachtwerk.essencium.backend.util;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {
  /**
   * Checks whether the current transaction is running within a user context.
   *
   * @return
   *     <ul>
   *       <li>empty {@link Optional} if invoked by any non-user-service (Scheduler, Initializer,
   *           Async-Jobs, ...)
   *       <li>{@link Optional< EssenciumUserDetails >} if current Transaction is executed in a User
   *           context
   *     </ul>
   */
  public static Optional<EssenciumUserDetails<? extends Serializable>>
      getUserDetailsFromAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.nonNull(authentication)
        && Objects.nonNull(authentication.getPrincipal())
        && authentication.getPrincipal() instanceof EssenciumUserDetails<?> userDetails) {
      return Optional.of(userDetails);
    }
    return Optional.empty();
  }

  public static HashSet<String> getRightsFromUserDetails(
      EssenciumUserDetails<? extends Serializable> userDetails) {
    return userDetails.getRights().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toCollection(HashSet::new));
  }

  public static boolean hasRole(
      EssenciumUserDetails<? extends Serializable> userDetails, String role) {
    if (Objects.isNull(userDetails) || Objects.isNull(role)) {
      return false;
    }
    return userDetails.getRoles().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(s -> Objects.equals(s, role));
  }

  public static boolean hasRight(
      EssenciumUserDetails<? extends Serializable> userDetails, String right) {
    if (Objects.isNull(userDetails) || Objects.isNull(right)) {
      return false;
    }
    return userDetails.getRights().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(s -> Objects.equals(s, right));
  }
}
