package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTokenInvalidationService {

  private static final Logger LOG = LoggerFactory.getLogger(SessionTokenInvalidationService.class);
  private final SessionTokenRepository sessionTokenRepository;

  @Autowired
  public SessionTokenInvalidationService(SessionTokenRepository sessionTokenRepository) {
    this.sessionTokenRepository = sessionTokenRepository;
  }

  @Transactional
  public void invalidateTokensForUser(String username) {
    LOG.info("Invalidating all session tokens for user '{}'.", username);
    try {
      sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCaseAndExpirationAfter(
          username, new Date());
      LOG.debug("All tokens for user '{}' successfully invalidated.", username);
    } catch (Exception e) {
      LOG.error("Error invalidating tokens for user '{}': {}", username, e.getMessage(), e);
      // Re-throw or handle as per your error handling strategy
      throw new RuntimeException("Failed to invalidate tokens for user " + username, e);
    }
  }
}
