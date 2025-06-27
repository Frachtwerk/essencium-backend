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
import de.frachtwerk.essencium.backend.model.dto.JwtRoleRights;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthenticationToken<ID extends Serializable>
    extends UsernamePasswordAuthenticationToken {

  public JwtAuthenticationToken(Claims claims, List<JwtRoleRights> roleRights) {
    super(createPrincipal(claims, roleRights), claims, buildAuthorities(roleRights));
  }

  @SuppressWarnings("unchecked")
  private static <ID extends Serializable> EssenciumUserDetailsImpl<ID> createPrincipal(
      Claims c, List<JwtRoleRights> roleRights) {
    List<JwtRoleRights> rolesWithRights =
        roleRights.stream().map(r -> new JwtRoleRights(r.getRole(), r.getRights())).toList();
    Map<String, Object> otherClaims = new HashMap<>(Map.copyOf(c));
    otherClaims.remove(JwtTokenService.CLAIM_UID);
    otherClaims.remove(JwtTokenService.CLAIM_ROLES);
    otherClaims.remove(JwtTokenService.CLAIM_FIRST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LAST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LOCALE);
    ID uid = (ID) c.get(JwtTokenService.CLAIM_UID);
    return new EssenciumUserDetailsImpl<>(
        uid,
        c.getSubject(),
        c.get(JwtTokenService.CLAIM_FIRST_NAME, String.class),
        c.get(JwtTokenService.CLAIM_LAST_NAME, String.class),
        c.get(JwtTokenService.CLAIM_LOCALE, String.class),
        rolesWithRights,
        otherClaims);
  }

  @SuppressWarnings("unchecked")
  public static Collection<? extends GrantedAuthority> buildAuthorities(List<JwtRoleRights> roles) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    for (JwtRoleRights rwr : roles) {
      authorities.add(new SimpleGrantedAuthority(rwr.getRole()));
      for (String right : rwr.getRights()) {
        authorities.add(new SimpleGrantedAuthority(right));
      }
    }

    return authorities;
  }

  public static JwtRoleRights mapToRoleWithRights(Map<String, Object> map) {
    JwtRoleRights rwr = new JwtRoleRights();
    rwr.setRole((String) map.get("role"));

    Object rights = map.get("rights");
    if (rights instanceof List<?>) {
      Set<String> safeRights =
          ((List<?>) rights)
              .stream()
                  .filter(String.class::isInstance)
                  .map(String.class::cast)
                  .collect(Collectors.toSet());
      rwr.setRights(safeRights);
    }

    return rwr;
  }
}
