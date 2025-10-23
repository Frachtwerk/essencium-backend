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
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TokenInvalidationService {

  private final SessionTokenRepository sessionTokenRepository;
  private final ApiTokenRepository apiTokenRepository;
  private final BaseUserRepository baseUserRepository;
  private final RoleRepository roleRepository;
  private final UserStateService userStateService;

  @Autowired
  public TokenInvalidationService(
      SessionTokenRepository sessionTokenRepository,
      ApiTokenRepository apiTokenRepository,
      BaseUserRepository baseUserRepository,
      RoleRepository roleRepository,
      UserStateService userStateService) {
    this.sessionTokenRepository = sessionTokenRepository;
    this.apiTokenRepository = apiTokenRepository;
    this.baseUserRepository = baseUserRepository;
    this.roleRepository = roleRepository;
    this.userStateService = userStateService;
  }

  @Transactional
  public void invalidateTokensForUserByUsername(String username) {
    log.info("Invalidating all session tokens for user '{}'.", username);
    try {
      sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(username);
      apiTokenRepository
          .findAllByLinkedUser(username)
          .forEach(
              apiToken -> {
                sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(apiToken.getUsername());
                apiTokenRepository.delete(apiToken);
              });
      log.debug("All tokens for user '{}' successfully invalidated.", username);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for user " + username, e);
    }
  }

  @Transactional
  public <ID extends Serializable> void invalidateTokensForUserByID(ID id) {
    Optional<? extends AbstractBaseUser<ID>> userOptional = baseUserRepository.findById(id);
    if (userOptional.isPresent()) {
      invalidateTokensForUserByUsername(userOptional.get().getUsername());
    } else {
      throw new TokenInvalidationException("No user found with ID " + id);
    }
  }

  @Transactional
  public void invalidateTokensOnUserUpdate(AbstractBaseUser<?> updatedUser) {
    try {
      Optional<AbstractBaseUser<?>> originalUser =
          userStateService.fetchOriginalUserState(updatedUser);

      AbstractBaseUser<?> user = originalUser.orElse(updatedUser);

      if ((originalUser.isPresent() && hasRelevantChanges(originalUser.get(), updatedUser))
          || (originalUser.isEmpty() && Objects.nonNull(updatedUser))) {
        log.info("Invalidating tokens for user: {}", user.getUsername());
        sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(user.getEmail());
        apiTokenRepository
            .findAllByLinkedUser(user.getEmail())
            .forEach(
                apiToken -> {
                  sessionTokenRepository.deleteAllByUsernameEqualsIgnoreCase(
                      apiToken.getUsername());
                  apiTokenRepository.delete(apiToken);
                });
      }
    } catch (Exception e) {
      throw new TokenInvalidationException(
          "Failed to invalidate tokens for user mit ID " + updatedUser.getId(), e);
    }
  }

  @Transactional
  public void invalidateTokensForRole(String roleName, Role roleToSave) {
    Role currentRole = roleRepository.findByName(roleName);
    if (Objects.nonNull(roleToSave) && Objects.nonNull(currentRole)) {
      if (roleToSave.getRights().containsAll(currentRole.getRights())) {
        log.debug(
            "No relevant changes detected for role '{}'. Token invalidation skipped.", roleName);
        return;
      }
    }

    log.info("Invalidating all session tokens for role '{}'.", roleName);
    try {
      List<String> allByRole = baseUserRepository.findAllUsernamesByRole(roleName);
      allByRole.forEach(this::invalidateTokensForUserByUsername);
      log.debug("All tokens for role '{}' successfully invalidated.", roleName);
    } catch (DataIntegrityViolationException dataIntegrityViolationException) {
      throw dataIntegrityViolationException;
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for role " + roleName, e);
    }
  }

  public void invalidateTokensForRoleDeletion(String roleName) {
    List<String> allByRole = baseUserRepository.findAllUsernamesByRole(roleName);
    if (!allByRole.isEmpty()) {
      throw new DataIntegrityViolationException(
          "Role is still in use by %d users".formatted(allByRole.size()));
    }
  }

  @Transactional
  public void invalidateTokensForRight(String right) {
    log.info("Invalidating all session tokens for right '{}'.", right);
    try {
      List<String> allByRight = baseUserRepository.findAllUsernamesByRight(right);
      allByRight.forEach(this::invalidateTokensForUserByUsername);
      log.debug("All tokens for right '{}' successfully invalidated.", right);
    } catch (Exception e) {
      throw new TokenInvalidationException("Failed to invalidate tokens for right " + right, e);
    }
  }

  public boolean hasRelevantChanges(
      AbstractBaseUser<?> originalUser, AbstractBaseUser<?> currentUser) {
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
