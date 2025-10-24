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

import de.frachtwerk.essencium.backend.configuration.initialization.DataInitializer;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.TokenInvalidationService;
import jakarta.annotation.Nullable;
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
      "execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*save*(..))"
          + " && !withinInitializationPackage()")
  public void rightModificationMethods() {}

  @Pointcut("execution(* de.frachtwerk.essencium.backend.repository.RightRepository+.*delete*(..))")
  public void rightDeletionMethods() {}

  @Before("userModificationMethods()")
  public void beforeUserModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping user token invalidation - called from DataInitializer");
      return;
    }

    List<?> extractEntities =
        extractEntities(joinPoint, AbstractBaseUser.class, Serializable.class);
    if (extractEntities.isEmpty()) {
      return;
    }
    Object first = extractEntities.getFirst();
    if (Objects.requireNonNull(first) instanceof AbstractBaseUser<?>) {
      log.debug("beforeUserModification - Users to process: {}", extractEntities.size());
      for (Object entity : extractEntities) {
        if (entity instanceof AbstractBaseUser<?> user && Objects.nonNull(user.getId())) {
          tokenInvalidationService.invalidateTokensOnUserUpdate(user);
        }
      }
    } else throw new IllegalStateException("Unexpected value: " + first);
  }

  @Before("userDeletionMethods()")
  public void beforeUserDeletion(JoinPoint joinPoint) {
    List<?> extractEntities =
        extractEntities(joinPoint, AbstractBaseUser.class, Serializable.class);
    if (extractEntities.isEmpty()) {
      return;
    }

    Object first = extractEntities.getFirst();
    switch (first) {
      case AbstractBaseUser<?> user -> {
        for (Object item : extractEntities) {
          AbstractBaseUser<?> u = (AbstractBaseUser<?>) item;
          tokenInvalidationService.invalidateTokensForUserByUsername(u.getUsername());
        }
      }
      case Serializable id -> {
        for (Object item : extractEntities) {
          Serializable userId = (Serializable) item;
          tokenInvalidationService.invalidateTokensForUserByID(userId);
        }
      }
      default -> throw new IllegalStateException("Unexpected value: " + first);
    }
  }

  @Before("roleModificationMethods()")
  public void beforeRoleModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping role token invalidation - called from DataInitializer");
      return;
    }

    List<?> extractEntities = extractEntities(joinPoint, Role.class, String.class);
    if (extractEntities.isEmpty()) {
      return;
    }
    Object first = extractEntities.getFirst();
    if (Objects.requireNonNull(first) instanceof Role) {
      log.debug("beforeRoleModification - Roles to process: {}", extractEntities.size());
      for (Object item : extractEntities) {
        invalidateUsersByRole((Role) item);
      }
    } else throw new IllegalStateException("Unexpected value: " + first);
  }

  @Before("roleDeletionMethods()")
  public void beforeRoleDeletion(JoinPoint joinPoint) {
    List<?> extractEntities = extractEntities(joinPoint, Role.class, String.class);
    if (extractEntities.isEmpty()) {
      return;
    }

    Object first = extractEntities.getFirst();
    switch (first) {
      case Role role -> {
        for (Object item : extractEntities) {
          invalidateUsersByRoleDeletion((Role) item);
        }
      }
      case String roleId -> {
        for (Object item : extractEntities) {
          invalidateUsersByRoleDeletionId((String) item);
        }
      }
      default -> throw new IllegalStateException("Unexpected value: " + first);
    }
  }

  @Before("rightModificationMethods()")
  public void beforeRightModification(JoinPoint joinPoint) {
    if (isCalledFromDataInitializer()) {
      log.debug("Skipping right token invalidation - called from DataInitializer");
      return;
    }

    List<?> extractEntities = extractEntities(joinPoint, Right.class, String.class);
    if (extractEntities.isEmpty()) {
      return;
    }
    Object first = extractEntities.getFirst();
    if (Objects.requireNonNull(first) instanceof Right) {
      log.debug("beforeRightModification - Rights to process: {}", extractEntities.size());
      for (Object item : extractEntities) {
        invalidateUsersByRight((Right) item);
      }
    } else throw new IllegalStateException("Unexpected value: " + first);
  }

  @Before("rightDeletionMethods()")
  public void beforeRightDeletion(JoinPoint joinPoint) {
    List<?> extractEntities = extractEntities(joinPoint, Right.class, String.class);
    if (extractEntities.isEmpty()) {
      return;
    }

    Object first = extractEntities.getFirst();
    switch (first) {
      case Right right -> {
        for (Object item : extractEntities) {
          invalidateUsersByRightDeletion((Right) item);
        }
      }
      case String authority -> {
        for (Object item : extractEntities) {
          invalidateUsersByRightDeletionId((String) item);
        }
      }
      default -> throw new IllegalStateException("Unexpected value: " + first);
    }
  }

  private <T, ID extends Serializable> List<?> extractEntities(
      JoinPoint joinPoint, Class<T> classType, Class<ID> idType) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 0) {
      return new ArrayList<T>();
    }

    Object arg = args[0];
    if (arg instanceof Iterable<?> iterable) {
      if (iterable.iterator().hasNext()) {
        Object firstItem = iterable.iterator().next();
        if (classType.isInstance(firstItem)) {
          List<T> entities = new ArrayList<>();
          for (Object item : iterable) {
            entities.add(classType.cast(item));
          }
          return entities;
        } else if (idType.isInstance(firstItem)) {
          List<ID> ids = new ArrayList<>();
          for (Object item : iterable) {
            ids.add(idType.cast(item));
          }
          return ids;
        } else {
          log.warn("Unexpected type in collection: {}", firstItem.getClass().getSimpleName());
          return new ArrayList<T>();
        }
      }
    } else if (classType.isInstance(arg)) {
      return List.of(classType.cast(arg));
    } else if (idType.isInstance(arg)) {
      return List.of(idType.cast(arg));
    }
    log.warn("Unexpected type for argument: {}", arg.getClass().getSimpleName());
    return new ArrayList<>();
  }

  private void invalidateUsersByRole(@Nullable Role role) {
    if (role != null && role.getName() != null) {
      String roleName = role.getName();
      log.info("Role modification detected: {}", roleName);
      tokenInvalidationService.invalidateTokensForRole(roleName, role);
    } else {
      log.warn("Role or role name is null, token invalidation skipped");
    }
  }

  private void invalidateUsersByRoleDeletion(@Nullable Role role) {
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

  private void invalidateUsersByRight(@Nullable Right right) {
    if (right != null && right.getAuthority() != null) {
      String authority = right.getAuthority();
      log.info("Right modification detected: {}", authority);
      tokenInvalidationService.invalidateTokensForRight(authority, right);
    } else {
      log.warn("Right or authority is null, token invalidation skipped");
    }
  }

  private void invalidateUsersByRightDeletion(@Nullable Right right) {
    if (right != null && right.getAuthority() != null) {
      String rightName = right.getAuthority();
      log.info("Right deletion detected: {}", rightName);
      tokenInvalidationService.invalidateTokensForRightDeletion(rightName);
    } else {
      log.warn("Right or authority is null, token invalidation skipped");
    }
  }

  private void invalidateUsersByRightDeletionId(Serializable id) {
    tokenInvalidationService.invalidateTokensForRightDeletion((String) id);
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
        // -> probably a DataInitializer
        try {
          Class<?> clazz = Class.forName(className);
          // Check if the class implements DataInitializer (directly or through inheritance)
          if (DataInitializer.class.isAssignableFrom(clazz)) {
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
