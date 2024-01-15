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

package de.frachtwerk.essencium.backend.security.permission;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.io.Serializable;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionEvaluator<ID extends Serializable, USER extends AbstractBaseUser<ID>>
    implements EntityPermissionEvaluator {

  private static final Set<Class<?>> SUPPORTED_CLASSES = Set.of(Role.class);

  private final RoleService roleService;

  @Autowired
  public RolePermissionEvaluator(RoleService roleService) {
    this.roleService = roleService;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    final USER user = (USER) authentication.getPrincipal();
    if (permission.equals("create")) {
      return user.hasAuthority(BasicApplicationRight.ROLE_CREATE);
    }
    return false;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    final USER user = (USER) authentication.getPrincipal();

    if (!targetType.equals(Role.class.getSimpleName())) {
      return false;
    }

    try {
      return switch ((String) permission) {
        case "read" -> user.hasAuthority(BasicApplicationRight.ROLE_READ);
        case "update" -> user.hasAuthority(BasicApplicationRight.ROLE_UPDATE)
            && !roleService.getByName((String) targetId).isProtected();
        case "delete" -> user.hasAuthority(BasicApplicationRight.ROLE_DELETE)
            && !roleService.getByName((String) targetId).isProtected();
        default -> false;
      };
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean supports(String type) {
    return SUPPORTED_CLASSES.stream().map(Class::getSimpleName).anyMatch(s -> s.equals(type));
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return SUPPORTED_CLASSES.contains(clazz);
  }
}
