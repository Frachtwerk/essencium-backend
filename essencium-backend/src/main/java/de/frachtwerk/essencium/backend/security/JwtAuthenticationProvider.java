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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetailsImpl;
import de.frachtwerk.essencium.backend.model.dto.RightGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.RoleGrantedAuthority;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/** Provider to fetch user details for a previously extracted and validated JWT token */
public class JwtAuthenticationProvider<
        USER extends AbstractBaseUser<ID>,
        JWTUSER extends EssenciumUserDetailsImpl<ID>,
        ID extends Serializable,
        USERDTO extends UserDto<ID>>
    extends AbstractUserDetailsAuthenticationProvider {

  @Autowired private AbstractUserService<USER, JWTUSER, ID, USERDTO> userService;

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
      throws AuthenticationException {}

  /** Build a minimal user object from the JWT – no DB lookup here */
  @Override
  protected EssenciumUserDetailsImpl<ID> retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {

    Claims claims = (Claims) authentication.getCredentials();
    Map<String, Object> otherClaims = new HashMap<>(Map.copyOf(claims));
    otherClaims.remove(JwtTokenService.CLAIM_UID);
    otherClaims.remove(JwtTokenService.CLAIM_ROLES);
    otherClaims.remove(JwtTokenService.CLAIM_RIGHTS);
    otherClaims.remove(JwtTokenService.CLAIM_FIRST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LAST_NAME);
    otherClaims.remove(JwtTokenService.CLAIM_LOCALE);
    ObjectMapper mapper = new ObjectMapper();
    ID uid = mapper.convertValue(claims.get(JwtTokenService.CLAIM_UID), new TypeReference<ID>() {});
    return EssenciumUserDetailsImpl.<ID>builder()
        .id(uid)
        .username(claims.getSubject())
        .firstName(claims.get(JwtTokenService.CLAIM_FIRST_NAME, String.class))
        .lastName(claims.get(JwtTokenService.CLAIM_LAST_NAME, String.class))
        .locale(claims.get(JwtTokenService.CLAIM_LOCALE, String.class))
        .roles(
            ((Set<RoleGrantedAuthority>) claims.get(JwtTokenService.CLAIM_ROLES))
                .stream()
                    .map(role -> new RoleGrantedAuthority(role.toString()))
                    .collect(Collectors.toSet()))
        .rights(
            ((Set<RightGrantedAuthority>) claims.get(JwtTokenService.CLAIM_RIGHTS))
                .stream()
                    .map(right -> new RightGrantedAuthority(right.toString()))
                    .collect(Collectors.toSet()))
        .additionalClaims(otherClaims)
        .additionalClaims(otherClaims)
        .build();
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return aClass.equals(JwtAuthenticationToken.class);
  }
}
