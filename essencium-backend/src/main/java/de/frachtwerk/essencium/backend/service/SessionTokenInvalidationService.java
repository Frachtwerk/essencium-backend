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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SessionTokenInvalidationService {

  private final SessionTokenRepository sessionTokenRepository;
  private final BaseUserRepository baseUserRepository;
  private final UserStateService userStateService;

  @Autowired
  public SessionTokenInvalidationService(
      SessionTokenRepository sessionTokenRepository,
      BaseUserRepository baseUserRepository,
      UserStateService userStateService) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.baseUserRepository = baseUserRepository;
    this.userStateService = userStateService;
  }

  @Transactional
  public void invalidateTokensForUserByUsername(String username) {
    log.info("Invalidating all session tokens for user '{}'.", username);
    try {
      sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(username);
      log.debug("All tokens for user '{}' successfully invalidated.", username);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for user " + username, e);
    }
  }

  @Transactional
  public void invalidateTokensOnUserUpdate(AbstractBaseUser<?> updatedUser) {
    try {
      AbstractBaseUser<?> originalUser = userStateService.fetchOriginalUserState(updatedUser);

      if (Objects.nonNull(originalUser) && hasRelevantChanges(originalUser, updatedUser)) {
        log.info("Invalidating tokens for user: {}", originalUser.getUsername());
        sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(originalUser.getEmail());

      } else if (Objects.nonNull(updatedUser)) {
        log.info(
            "Could not fetch original user state for user: {}, invalidating tokens for updated user",
            updatedUser.getUsername());
        sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(updatedUser.getEmail());
      } else {
        log.info(
            "No relevant changes detected for user: {}, skipping token invalidation",
            updatedUser.getUsername());
      }
    } catch (Exception e) {
      throw new TokenInvalidationException(
          "Failed to invalidate tokens for user mit ID " + updatedUser.getId(), e);
    }
  }

  @Transactional
  public void invalidateTokensForRole(String roleName) {
    log.info("Invalidating all session tokens for role '{}'.", roleName);
    try {
      List<String> allByRole = baseUserRepository.findAllUsernamesByRole(roleName);
      allByRole.stream().forEach(this::invalidateTokensForUserByUsername);
      log.debug("All tokens for role '{}' successfully invalidated.", roleName);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for role " + roleName, e);
    }
  }

  @Transactional
  public void invalidateTokensForRight(String right) {
    log.info("Invalidating all session tokens for right '{}'.", right);
    try {
      List<String> allByRight = baseUserRepository.findAllUsernamesByRight(right);
      allByRight.stream().forEach(this::invalidateTokensForUserByUsername);
      log.debug("All tokens for right '{}' successfully invalidated.", right);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for right " + right, e);
    }
  }

  public boolean hasRelevantChanges(AbstractBaseUser originalUser, AbstractBaseUser currentUser) {
    if (Objects.nonNull(originalUser) && Objects.nonNull(currentUser)) {

      return !Objects.equals(originalUser.getEmail(), currentUser.getEmail())
          || !Objects.equals(originalUser.getLocale(), currentUser.getLocale())
          || !Objects.equals(originalUser.getRoles(), currentUser.getRoles())
          || originalUser.isEnabled() != currentUser.isEnabled()
          || originalUser.isAccountNonLocked() != currentUser.isAccountNonLocked()
          || !Objects.equals(originalUser.getSource(), currentUser.getSource());
    }
    return true;
  }
}
