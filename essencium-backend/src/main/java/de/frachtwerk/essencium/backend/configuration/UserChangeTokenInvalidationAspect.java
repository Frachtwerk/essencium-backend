package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.SessionTokenInvalidationService;
import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserChangeTokenInvalidationAspect {

  private static final Logger LOG =
      LoggerFactory.getLogger(UserChangeTokenInvalidationAspect.class);

  private final SessionTokenInvalidationService sessionTokenInvalidationService;

  public UserChangeTokenInvalidationAspect(
      SessionTokenInvalidationService sessionTokenInvalidationService) {
    this.sessionTokenInvalidationService = sessionTokenInvalidationService;
  }

  @Pointcut("execution(* de.frachtwerk.essencium.backend.configuration.initialization.*.*(..))")
  public void ignoreInitializer() {}

  @Before("ignoreInitializer()")
  public void ignoreInitializerMethods(JoinPoint joinPoint) {
    LOG.debug("Ignoring initialization method: {}", joinPoint.getSignature().getName());
  }

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

  @Before("userModificationMethods()")
  public void beforeUserModification(JoinPoint joinPoint) {
    List<AbstractBaseUser> users = extractEntities(joinPoint, AbstractBaseUser.class);
    for (AbstractBaseUser user : users) {
      invalidateUserTokens(user);
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
          LOG.warn("Unexpected type in collection: {}", item.getClass().getSimpleName());
        }
      }
    } else {
      LOG.warn(
          "Unexpected argument type: {} for method: {}",
          arg.getClass().getSimpleName(),
          joinPoint.getSignature().getName());
    }

    return entities;
  }

  protected void invalidateUserTokens(AbstractBaseUser<?> user) {
    if (user == null || user.getUsername() == null) {
      LOG.warn("User or username is null, skipping token invalidation");
      return;
    }

    String username = user.getUsername();
    sessionTokenInvalidationService.invalidateTokensForUser(username);
    LOG.info("Invalidated tokens for user: {}", username);
  }

  protected void invalidateUsersByRole(Role role) {
    if (role == null || role.getName() == null) {
      LOG.warn("Role or role name is null, skipping token invalidation");
      return;
    }

    String roleName = role.getName();
    LOG.info("Role modification detected: {}", roleName);
    sessionTokenInvalidationService.invalidateTokensForRole(roleName);
    LOG.info("Invalidated tokens for role: {}", roleName);
  }

  protected void invalidateUsersByRight(Right right) {
    if (right == null || right.getAuthority() == null) {
      LOG.warn("Right or authority is null, skipping token invalidation");
      return;
    }

    String authority = right.getAuthority();
    LOG.info("Right modification detected: {}", authority);
    sessionTokenInvalidationService.invalidateTokensForRight(authority);
    LOG.info("Invalidated tokens for right: {}", authority);
  }
}
