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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.SessionTokenInvalidationService;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class UserTokenInvalidationAspect {

  private final SessionTokenInvalidationService sessionTokenInvalidationService;

  public UserTokenInvalidationAspect(
      @NotNull SessionTokenInvalidationService sessionTokenInvalidationService) {
    this.sessionTokenInvalidationService = sessionTokenInvalidationService;
  }

  @Pointcut("execution(* de.frachtwerk.essencium.backend.configuration.initialization..*(..))")
  public void ignoreInitializer() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.BaseUserRepository+.*save*(..))")
  public void userModificationMethods() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.RoleRepository+.*save*(..))"
          + " || execution(* de.frachtwerk.essencium.backend.repository.RoleRepository+.*delete*(..))")
  public void roleModificationMethods() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*save*(..))"
          + " || execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*delete*(..))")
  public void rightModificationMethods() {}

  @Before("ignoreInitializer()")
  public void ignoreInitializerMethods(JoinPoint joinPoint) {
    log.debug("Ignoring initialization method: {}", joinPoint.getSignature().getName());
  }

  @Before("userModificationMethods()")
  public void beforeUserModification(JoinPoint joinPoint) throws Throwable {
    List<AbstractBaseUser> users = extractEntities(joinPoint, AbstractBaseUser.class);

    for (AbstractBaseUser user : users) {
      if (Objects.nonNull(user.getId())) {
        sessionTokenInvalidationService.invalidateTokensOnUserUpdate(user);
      }
    }
  }

  @Before("roleModificationMethods()")
  public void beforeRoleModification(JoinPoint joinPoint) {
    List<Role> roles = extractEntities(joinPoint, Role.class);
    for (Role role : roles) {
      invalidateUsersByRole(role);
    }
  }

  @Before("rightModificationMethods()")
  public void beforeRightModification(JoinPoint joinPoint) {
    List<Right> rights = extractEntities(joinPoint, Right.class);
    for (Right right : rights) {
      invalidateUsersByRight(right);
    }
  }

  private <T> List<T> extractEntities(JoinPoint joinPoint, Class<T> expectedType) {
    List<T> entities = new ArrayList<>();
    Object[] args = joinPoint.getArgs();

    if (args.length == 0) {
      return entities;
    }

    Object arg = args[0];

    if (expectedType.isInstance(arg)) {
      entities.add(expectedType.cast(arg));
    } else if (arg instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (expectedType.isInstance(item)) {
          entities.add(expectedType.cast(item));
        } else {
          log.warn("Unexpected type in collection: {}", item.getClass().getSimpleName());
        }
      }
    } else {
      log.warn(
          "Unexpected argument type: {} for method: {}",
          arg.getClass().getSimpleName(),
          joinPoint.getSignature().getName());
    }

    return entities;
  }

  protected void invalidateUsersByRole(Role role) {
    if (role != null && role.getName() != null) {
      String roleName = role.getName();
      log.info("Role modification detected: {}", roleName);
      sessionTokenInvalidationService.invalidateTokensForRole(roleName);
    } else {
      log.warn("Role or role name is null, token invalidation skipped");
    }
  }

  protected void invalidateUsersByRight(Right right) {
    if (right != null && right.getAuthority() != null) {
      String authority = right.getAuthority();
      log.info("Right modification detected: {}", authority);
      sessionTokenInvalidationService.invalidateTokensForRight(authority);
    } else {
      log.warn("Right or authority is null, token invalidation skipped");
    }
  }
}
