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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.lang.Assert;
import java.io.Serializable;
import java.util.Optional;
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
    Assert.isInstanceOf(JwtAuthenticationToken.class, authentication);
    Assert.isInstanceOf(AbstractBaseUser.class, userDetails);

    // discussion about token blacklist for server-side invalidation:
    // https://stackoverflow.com/questions/47224931/is-setting-roles-in-jwt-a-best-practice/53527119#53527119

    Optional<String> requestedNonce =
        Optional.ofNullable(
            ((Claims) authentication.getCredentials())
                .get(JwtTokenService.CLAIM_NONCE, String.class));
    Optional<String> actualNonce = Optional.ofNullable(((USER) userDetails).getNonce());

    if (actualNonce.isEmpty()) {
      LOGGER.warn(
          "security nonce missing in database for user {} – you should set one!",
          userDetails.getUsername());
    }

    if (!requestedNonce.equals(actualNonce)) {
      authentication
          .eraseCredentials(); // implicitly stop subsequent providers from trying to evaluate this
      // token
      throw new NonceExpiredException("nonce expired");
    }
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {
    return userService.loadUserByUsername(username);
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return aClass.equals(JwtAuthenticationToken.class);
  }
}
