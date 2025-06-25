package de.frachtwerk.essencium.backend.configuration;

import org.aspectj.lang.annotation.*;

//
// @Aspect
// @Component
public class UserChangeTokenInvalidationAspect {
  //
  //  private static final Logger LOG =
  //      LoggerFactory.getLogger(UserChangeTokenInvalidationAspect.class);
  //
  //  private final JwtTokenService jwtTokenService;
  //  private final SessionTokenRepository sessionTokenRepository;
  //
  //  @Autowired
  //  public UserChangeTokenInvalidationAspect(
  //      JwtTokenService jwtTokenService, SessionTokenRepository sessionTokenRepository) {
  //    this.jwtTokenService = jwtTokenService;
  //    this.sessionTokenRepository = sessionTokenRepository;
  //  }
  //
  //  // Pointcut auf alle Methoden, die einen User speichern/aktualisieren
  //  @Pointcut(
  //      "execution(* de.frachtwerk.essencium.backend.service.AbstractUserService.save(..)) || "
  //          + "execution(*
  // de.frachtwerk.essencium.backend.service.AbstractUserService.updatePreProcessing(..)) || "
  //          + "execution(*
  // de.frachtwerk.essencium.backend.service.AbstractUserService.patchPreProcessing(..)) || "
  //          + "execution(*
  // de.frachtwerk.essencium.backend.service.AbstractUserService.updatePassword(..)) || "
  //          + "execution(*
  // de.frachtwerk.essencium.backend.service.AbstractUserService.selfUpdate(..))")
  //  public void userModificationMethods() {}
  //
  //  @AfterReturning(pointcut = "userModificationMethods()", returning = "result")
  //  public void afterUserModification(JoinPoint joinPoint, Object result) {
  //    if (result instanceof AbstractBaseUser<?> updatedUser) {
  //      String username = updatedUser.getEmail();
  //      LOG.info("User '{}' wurde geändert - invalidiere alle Session Tokens.", username);
  //
  //      try {
  //        // delete all ACCESS_TOKENs that belong to this REFRESH_TOKEN
  //        jwtTokenService.invalidateAllTokensForUser(username);
  //        LOG.debug("Alle Tokens für User '{}' erfolgreich invalidiert.", username);
  //      } catch (Exception e) {
  //        LOG.error(
  //            "Fehler beim Invalidieren der Tokens für User '{}': {}", username, e.getMessage(),
  // e);
  //      }
  //    } else {
  //      LOG.warn("Erwartetes Rückgabeobjekt AbstractBaseUser, aber erhalten: {}", result);
  //    }
  //  }
}
