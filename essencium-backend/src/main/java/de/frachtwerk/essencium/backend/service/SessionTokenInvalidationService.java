package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.List;
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
  public void invalidateTokensForUser(String username) {
    LOG.info("Invalidating all session tokens for user '{}'.", username);
    try {
      sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(username);
      LOG.debug("All tokens for user '{}' successfully invalidated.", username);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for user " + username, e);
    }
  }

  @Transactional
  public void invalidateTokensForRole(String roleName) {
    LOG.info("Invalidating all session tokens for role '{}'.", roleName);
    try {
      List<String> allByRole = baseUserRepository.findAllUsernamesByRole(roleName);
      allByRole.stream().forEach(this::invalidateTokensForUser);
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
      allByRight.stream().forEach(this::invalidateTokensForUser);
      LOG.debug("All tokens for right '{}' successfully invalidated.", right);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for right " + right, e);
    }
  }
}
