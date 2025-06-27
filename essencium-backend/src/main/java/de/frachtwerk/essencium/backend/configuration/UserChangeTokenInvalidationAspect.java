package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.service.SessionTokenInvalidationService; // Import this
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserChangeTokenInvalidationAspect {

  private static final Logger LOG =
      LoggerFactory.getLogger(UserChangeTokenInvalidationAspect.class);

  private final SessionTokenInvalidationService sessionTokenInvalidationService; // Use the service

  @Autowired
  public UserChangeTokenInvalidationAspect(
      SessionTokenInvalidationService sessionTokenInvalidationService) {
    this.sessionTokenInvalidationService = sessionTokenInvalidationService;
  }

  @Pointcut("execution(* de.frachtwerk.essencium.backend.repository.BaseUserRepository+.save(..))")
  public void userModificationMethods() {}

  @AfterReturning(pointcut = "userModificationMethods()", returning = "result")
  public void afterUserModification(JoinPoint joinPoint, Object result) {
    if (result instanceof AbstractBaseUser<?> updatedUser) {
      String username = updatedUser.getEmail();
      LOG.info("User '{}' wurde geändert - invalidiere alle Session Tokens.", username);

      // Call the service method which is transactional
      sessionTokenInvalidationService.invalidateTokensForUser(username);

    } else {
      LOG.warn("Erwartetes Rückgabeobjekt AbstractBaseUser, aber erhalten: {}", result);
    }
  }
}
