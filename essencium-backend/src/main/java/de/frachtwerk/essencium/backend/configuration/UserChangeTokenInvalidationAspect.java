package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.Date;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//
@Aspect
@Component // Ensure jwtTokenService is initialized before this aspect
public class UserChangeTokenInvalidationAspect {

  private static final Logger LOG =
      LoggerFactory.getLogger(UserChangeTokenInvalidationAspect.class);

  private final SessionTokenRepository jwtTokenService;
  private volatile boolean initializationDone = false;

  @Autowired
  public UserChangeTokenInvalidationAspect(SessionTokenRepository jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  // Pointcut and AfterReturning methods remain the same
  @Pointcut("execution(* de.frachtwerk.essencium.backend.repository.BaseUserRepository+.save(..))")

  // RightRole Repository + save/flush,...
  public void userModificationMethods() {}

  @AfterReturning(pointcut = "userModificationMethods()", returning = "result")
  public void afterUserModification(JoinPoint joinPoint, Object result) {
    if (result instanceof AbstractBaseUser<?> updatedUser) {
      String username = updatedUser.getEmail();
      LOG.info("User '{}' wurde geändert - invalidiere alle Session Tokens.", username);

      try {
        // delete all ACCESS_TOKENs that belong to this REFRESH_TOKEN
        jwtTokenService.deleteAllByUsernameEqualsIgnoreCaseAndExpirationBefore(
            username, new Date());
        LOG.debug("Alle Tokens für User '{}' erfolgreich invalidiert.", username);
      } catch (Exception e) {
        LOG.error(
            "Fehler beim Invalidieren der Tokens für User '{}': {}", username, e.getMessage(), e);
      }
    } else {
      LOG.warn("Erwartetes Rückgabeobjekt AbstractBaseUser, aber erhalten: {}", result);
    }
  }
}
