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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetailsImpl;
import de.frachtwerk.essencium.backend.model.dto.JwtRoleRights;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.www.NonceExpiredException;

/** Provider to fetch user details for a previously extracted and validated JWT token */
public class JwtAuthenticationProvider<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    extends AbstractUserDetailsAuthenticationProvider {

  @Autowired private AbstractUserService<USER, ID, USERDTO> userService;

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {

    Claims claims = (Claims) authentication.getCredentials();
    String tokenNonce = claims.get(JwtTokenService.CLAIM_NONCE, String.class);

    String currentNonce = userService.findNonceOnly(userDetails.getUsername());

    if (!Objects.equals(tokenNonce, currentNonce)) {
      authentication.eraseCredentials();
      throw new NonceExpiredException("nonce expired");
    }
  }

  /** Build a minimal user object from the JWT – no DB lookup here */
  @Override
  protected EssenciumUserDetailsImpl retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {

    Claims claims = (Claims) authentication.getCredentials();

    return new EssenciumUserDetailsImpl(
        claims.get(JwtTokenService.CLAIM_UID, Long.class),
        username,
        claims.get(JwtTokenService.CLAIM_FIRST_NAME, String.class),
        claims.get(JwtTokenService.CLAIM_LAST_NAME, String.class),
        extractRolesWithRights(claims));
  }

  @SuppressWarnings("unchecked")
  private List<JwtRoleRights> extractRolesWithRights(Claims claims) {
    List<Map<String, Object>> roles =
        (List<Map<String, Object>>) claims.get(JwtTokenService.CLAIM_ROLES);

    List<JwtRoleRights> roleDtos = new ArrayList<>();
    if (roles != null) {
      for (Map<String, Object> role : roles) {
        String roleName = (String) role.get("name");

        Set<String> rights = (Set<String>) role.get("rights");

        roleDtos.add(new JwtRoleRights(roleName, rights != null ? rights : Set.of()));
      }
    }

    return roleDtos;
  }

  //  private Collection<GrantedAuthority> extractAuthorities(Claims claims) {
  //    List<GrantedAuthority> authorities = new ArrayList<>();
  //
  //    @SuppressWarnings("unchecked")
  //    List<Map<String, Object>> roles =
  //        (List<Map<String, Object>>) claims.get(JwtTokenService.CLAIM_ROLES);
  // if (roles != null) {
  //  for (Map<String, Object> role : roles) {
  //    String roleName = (String) role.get("name");
  //    authorities.add(new SimpleGrantedAuthority(roleName));
  //
  //        @SuppressWarnings("unchecked")
  //        List<String> rights = (List<String>) role.get("rights");
  //        if (rights != null) {
  //          rights.forEach(right -> authorities.add(new SimpleGrantedAuthority(right)));
  //        }
  //      }
  //    }
  //
  //    return authorities;
  //  }

  @Override
  public boolean supports(Class<?> aClass) {
    return aClass.equals(JwtAuthenticationToken.class);
  }
}
