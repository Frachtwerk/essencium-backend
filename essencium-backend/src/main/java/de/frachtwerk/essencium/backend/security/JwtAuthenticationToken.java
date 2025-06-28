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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetailsImpl;
import de.frachtwerk.essencium.backend.model.dto.RightGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.RoleGrantedAuthority;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken<ID extends Serializable>
    extends UsernamePasswordAuthenticationToken {

  public JwtAuthenticationToken(
      Claims claims,
      List<? extends GrantedAuthority> roles,
      List<? extends GrantedAuthority> rights) {
    super(createPrincipal(claims), claims, buildAuthorities(roles, rights));
  }

  @SuppressWarnings("unchecked")
  private static <ID extends Serializable> EssenciumUserDetailsImpl<ID> createPrincipal(Claims c) {
    Map<String, Object> otherClaims = new HashMap<>(Map.copyOf(c));
    otherClaims.remove(JwtTokenService.CLAIM_UID);
    otherClaims.remove(JwtTokenService.CLAIM_ROLES);
    otherClaims.remove(JwtTokenService.CLAIM_RIGHTS);
    otherClaims.remove(JwtTokenService.CLAIM_FIRST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LAST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LOCALE);
    ID uid = (ID) c.get(JwtTokenService.CLAIM_UID);
    return EssenciumUserDetailsImpl.<ID>builder()
        .id(uid)
        .username(c.getSubject())
        .firstName(c.get(JwtTokenService.CLAIM_FIRST_NAME, String.class))
        .lastName(c.get(JwtTokenService.CLAIM_LAST_NAME, String.class))
        .locale(c.get(JwtTokenService.CLAIM_LOCALE, String.class))
        .roles(
            ((Collection<?>) c.get(JwtTokenService.CLAIM_ROLES))
                .stream()
                    .map(role -> new RoleGrantedAuthority(role.toString()))
                    .collect(Collectors.toSet()))
        .rights(
            ((Collection<?>) c.get(JwtTokenService.CLAIM_RIGHTS))
                .stream()
                    .map(right -> new RightGrantedAuthority(right.toString()))
                    .collect(Collectors.toSet()))
        .additionalClaims(otherClaims)
        .build();
  }

  public static Collection<? extends GrantedAuthority> buildAuthorities(
      List<? extends GrantedAuthority> roles, List<? extends GrantedAuthority> rights) {
    return Stream.concat(
            roles.stream().map(role -> new RoleGrantedAuthority(role.getAuthority())),
            rights.stream().map(right -> new RightGrantedAuthority(right.getAuthority())))
        .collect(Collectors.toSet());
  }
}
