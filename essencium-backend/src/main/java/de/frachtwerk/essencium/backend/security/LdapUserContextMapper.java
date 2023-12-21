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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.configuration.properties.LdapConfigProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LdapUserContextMapper<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    implements UserDetailsContextMapper {

  private final AbstractUserService<USER, ID, USERDTO> userService;
  private final RoleService roleService;
  private final LdapConfigProperties ldapConfigProperties;

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapUserContextMapper.class);

  @Override
  public UserDetails mapUserFromContext(
      DirContextOperations ctx,
      String username,
      Collection<? extends GrantedAuthority> authorities) {
    Set<Role> roles =
        authorities.stream()
            .filter(Role.class::isInstance)
            .map(r -> (Role) r)
            .collect(Collectors.toCollection(HashSet::new));
    Role defaultRole = roleService.getDefaultRole();
    if (roles.isEmpty() && Objects.nonNull(defaultRole)) {
      roles.add(defaultRole);
    }

    try {
      LOGGER.info("got successful ldap login for {}", username);

      final var user = userService.loadUserByUsername(username);

      if (ldapConfigProperties.isUpdateRole()) {
        user.setRoles(roles);
        userService.patch(Objects.requireNonNull(user.getId()), Map.of("roles", roles));
      }

      return user;
    } catch (UsernameNotFoundException e) {
      if (ldapConfigProperties.isAllowSignup()) {
        LOGGER.info("creating new user '{}' from successful ldap authentication", username);

        final var firstName =
            Optional.ofNullable(
                    ctx.getAttributes().get(ldapConfigProperties.getUserFirstnameAttr()))
                .map(a -> getAttrAsOrDefault(a, AbstractBaseUser.PLACEHOLDER_FIRST_NAME))
                .orElse(AbstractBaseUser.PLACEHOLDER_FIRST_NAME);

        final var lastName =
            Optional.ofNullable(ctx.getAttributes().get(ldapConfigProperties.getUserLastnameAttr()))
                .map(a -> getAttrAsOrDefault(a, AbstractBaseUser.PLACEHOLDER_LAST_NAME))
                .orElse(AbstractBaseUser.PLACEHOLDER_LAST_NAME);

        if (!ldapConfigProperties.getRoles().isEmpty() && roles.isEmpty()) {
          LOGGER.warn("ldap group mapping was specified, but no matching role could be found");
        }

        return userService.createDefaultUser(
            new UserInfoEssentials(username, firstName, lastName, roles),
            AbstractBaseUser.USER_AUTH_SOURCE_LDAP);
      }
    }
    throw new UsernameNotFoundException(String.format("%s not found locally", username));
  }

  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("not implemented");
  }

  @SuppressWarnings("unchecked")
  private static <T> T getAttrAsOrDefault(Attribute attribute, T defaultValue) {
    try {
      final var val = attribute.get();
      if (!defaultValue.getClass().isAssignableFrom(val.getClass())) {
        return defaultValue;
      }
      return (T) val;
    } catch (NamingException | ClassCastException e) {
      return defaultValue;
    }
  }
}
