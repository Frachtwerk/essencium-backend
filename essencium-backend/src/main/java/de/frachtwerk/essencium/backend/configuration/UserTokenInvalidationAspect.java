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
import de.frachtwerk.essencium.backend.service.TokenInvalidationService;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
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
  private final TokenInvalidationService tokenInvalidationService;

  public UserTokenInvalidationAspect(@NotNull TokenInvalidationService tokenInvalidationService) {
    this.tokenInvalidationService = tokenInvalidationService;
    log.info("UserTokenInvalidationAspect created");
  }

  @Pointcut("within(de.frachtwerk.essencium.backend.configuration.initialization..*)")
  public void withinInitializationPackage() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.BaseUserRepository+.*save*(..))"
          + " && !withinInitializationPackage()")
  public void userModificationMethods() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.BaseUserRepository+.*delete*(..))")
  public void userDeletionMethods() {}

  @Pointcut(
      "execution(* de.frachtwerk.essencium.backend.repository.RoleRepository+.*save*(..))"
          + " && !withinInitializationPackage()")
  public void roleModificationMethods() {}

  @Pointcut("execution(* de.frachtwerk.essencium.backend.repository.RoleRepository+.*delete*(..))")
  public void roleDeletionMethods() {}

  @Pointcut(
      "(execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*save*(..))"
          + " || execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*delete*(..)))"
          + " && !withinInitializationPackage()")
  public void rightModificationMethods() {}

  @Before("userModificationMethods()")
  public void beforeUserModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping user token invalidation - called from DataInitializer");
      return;
    }

    List<?> entities = extractEntities(joinPoint, AbstractBaseUser.class);
    log.debug("beforeUserModification - Users to process: {}", entities.size());

    for (Object entity : entities) {
      if (entity instanceof AbstractBaseUser<?> user && Objects.nonNull(user.getId())) {
        tokenInvalidationService.invalidateTokensOnUserUpdate(user);
      }
    }
  }

  @Before("userDeletionMethods()")
  public void beforeUserDeletion(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 0) {
      return;
    }

    Object arg = args[0];
    if (arg instanceof Iterable<?> iterable) {
      if (iterable.iterator().hasNext()) {
        Object firstItem = iterable.iterator().next();
        if (firstItem instanceof AbstractBaseUser<?> user) {
          for (Object item : iterable) {
            AbstractBaseUser<?> u = (AbstractBaseUser<?>) item;
            tokenInvalidationService.invalidateTokensForUserByUsername(u.getUsername());
          }
        } else if (firstItem instanceof Serializable id) {
          for (Object item : iterable) {
            Serializable userId = (Serializable) item;
            tokenInvalidationService.invalidateTokensForUserByID(userId);
          }
        } else {
          log.warn(
              "Unexpected type in collection for user deletion: {}",
              firstItem.getClass().getSimpleName());
        }
      }
    } else if (arg instanceof AbstractBaseUser<?> user) {
      tokenInvalidationService.invalidateTokensForUserByUsername(user.getUsername());
    } else if (arg instanceof Serializable id) {
      tokenInvalidationService.invalidateTokensForUserByID(id);
    }
  }

  @Before("roleModificationMethods()")
  public void beforeRoleModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping role token invalidation - called from DataInitializer");
      return;
    }

    List<Role> roles = extractEntities(joinPoint, Role.class);
    log.debug("beforeRoleModification - Roles to process: {}", roles.size());
    for (Role role : roles) {
      invalidateUsersByRole(role);
    }
  }

  @Before("roleDeletionMethods()")
  public void beforeRoleDeletion(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 0) {
      return;
    }

    Object arg = args[0];
    if (arg instanceof Iterable<?> iterable) {
      if (iterable.iterator().hasNext()) {
        Object firstItem = iterable.iterator().next();
        if (firstItem instanceof Role r) {
          for (Object item : iterable) {
            Role role = (Role) item;
            invalidateUsersByRoleDeletion(role);
          }
        } else if (firstItem instanceof Serializable id) {
          for (Object item : iterable) {
            String roleId = (String) item;
            invalidateUsersByRoleDeletionId(roleId);
          }
        } else {
          log.warn(
              "Unexpected type in collection for role deletion: {}",
              firstItem.getClass().getSimpleName());
        }
      }
    } else if (arg instanceof Role role) {
      invalidateUsersByRoleDeletion(role);
    } else if (arg instanceof Serializable id) {
      invalidateUsersByRoleDeletionId(id);
    }
  }

  @Before("rightModificationMethods()")
  public void beforeRightModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping right token invalidation - called from DataInitializer");
      return;
    }

    List<Right> rights = extractEntities(joinPoint, Right.class);
    log.debug("beforeRightModification - Rights to process: {}", rights.size());
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
      tokenInvalidationService.invalidateTokensForRole(roleName, role);
    } else {
      log.warn("Role or role name is null, token invalidation skipped");
    }
  }

  protected void invalidateUsersByRight(Right right) {
    if (right != null && right.getAuthority() != null) {
      String authority = right.getAuthority();
      log.info("Right modification detected: {}", authority);
      tokenInvalidationService.invalidateTokensForRight(authority);
    } else {
      log.warn("Right or authority is null, token invalidation skipped");
    }
  }

  private void invalidateUsersByRoleDeletion(Role role) {
    if (role != null && role.getName() != null) {
      String roleName = role.getName();
      log.info("Role deletion detected: {}", roleName);
      tokenInvalidationService.invalidateTokensForRoleDeletion(roleName);
    } else {
      log.warn("Role or role name is null, token invalidation skipped");
    }
  }

  private void invalidateUsersByRoleDeletionId(Serializable id) {
    tokenInvalidationService.invalidateTokensForRoleDeletion((String) id);
  }

  /**
   * Checks if the current method call is part of a DataInitializer execution by examining the call
   * stack. This method looks for DataInitializer implementations in the stack trace.
   */
  private boolean isCalledFromDataInitializer() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    for (StackTraceElement element : stackTrace) {
      String className = element.getClassName();
      if (className.contains("initialization")) {
        // -> propably a DataInitializer
        try {
          Class<?> clazz = Class.forName(className);
          // Check if the class implements DataInitializer (directly or through inheritance)
          if (de.frachtwerk.essencium.backend.configuration.initialization.DataInitializer.class
              .isAssignableFrom(clazz)) {
            return true;
          }
        } catch (ClassNotFoundException e) {
          // Ignore classes that can't be loaded
        }
      }
    }

    return false;
  }
}
