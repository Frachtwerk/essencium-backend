package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTokenInvalidationService {

  private static final Logger LOG = LoggerFactory.getLogger(SessionTokenInvalidationService.class);
  private final SessionTokenRepository sessionTokenRepository;
  private final BaseUserRepository baseUserRepository;

  @Autowired
  public SessionTokenInvalidationService(
      SessionTokenRepository sessionTokenRepository, BaseUserRepository baseUserRepository) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.baseUserRepository = baseUserRepository;
  }

  @Transactional
  public void invalidateTokensForUserByUsername(String username) {
    LOG.info("Invalidating all session tokens for user '{}'.", username);
    try {
      sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(username);
      LOG.debug("All tokens for user '{}' successfully invalidated.", username);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for user " + username, e);
    }
  }

  @Transactional
  public void invalidateTokensOnUserUpdate(AbstractBaseUser<?> updatedUser) {
    try {
      AbstractBaseUser<?> existingUser =
          (AbstractBaseUser<?>)
              baseUserRepository.getReferenceById(Objects.requireNonNull(updatedUser.getId()));

      boolean relevantFieldsChanged =
          !Objects.equals(existingUser.getEmail(), updatedUser.getEmail())
              || !Objects.equals(existingUser.getLocale(), updatedUser.getLocale())
              || !Objects.equals(existingUser.getRoles(), updatedUser.getRoles())
              || existingUser.isEnabled() != updatedUser.isEnabled()
              || existingUser.isAccountNonLocked() != updatedUser.isAccountNonLocked()
              || !Objects.equals(existingUser.getSource(), updatedUser.getSource());

      if (relevantFieldsChanged) {
        String username = existingUser.getEmail();
        sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(existingUser.getEmail());
        LOG.debug("All tokens for user '{}' successfully invalidated.", username);
      }
    } catch (Exception e) {
      throw new TokenInvalidationException(
          "Failed to invalidate tokens for user mit ID " + updatedUser.getId(), e);
    }
  }

  @Transactional
  public void invalidateTokensForRole(String roleName) {
    LOG.info("Invalidating all session tokens for role '{}'.", roleName);
    try {
      List<String> allByRole = baseUserRepository.findAllUsernamesByRole(roleName);
      allByRole.stream().forEach(this::invalidateTokensForUserByUsername);
      LOG.debug("All tokens for role '{}' successfully invalidated.", roleName);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for role " + roleName, e);
    }
  }

  @Transactional
  public void invalidateTokensForRight(String right) {
    LOG.info("Invalidating all session tokens for right '{}'.", right);
    try {
      List<String> allByRight = baseUserRepository.findAllUsernamesByRight(right);
      allByRight.stream().forEach(this::invalidateTokensForUserByUsername);
      LOG.debug("All tokens for right '{}' successfully invalidated.", right);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for right " + right, e);
    }
  }
}
