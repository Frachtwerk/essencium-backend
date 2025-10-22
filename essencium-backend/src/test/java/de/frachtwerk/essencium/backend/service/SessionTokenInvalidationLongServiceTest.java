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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTokenInvalidationService Tests")
class SessionTokenInvalidationLongServiceTest {

  @Mock SessionTokenRepository sessionTokenRepository;
  @Mock ApiTokenRepository apiTokenRepository;
  @Mock BaseUserRepository baseUserRepository;
  @Mock EntityManager entityManager;
  @InjectMocks UserStateService userStateService;
  @InjectMocks TokenInvalidationService tokenInvalidationService;

  private static final String TEST_USERNAME = "test@example.com";
  private static final String TEST_ROLE_NAME = "ADMIN";
  private static final String TEST_RIGHT_NAME = "READ_USERS";
  private static final Long TEST_USER_ID = 1L;

  @BeforeEach
  void setUp() {
    userStateService = new UserStateService(baseUserRepository);
    ReflectionTestUtils.setField(userStateService, "entityManager", entityManager);
    tokenInvalidationService =
        new TokenInvalidationService(
            sessionTokenRepository, apiTokenRepository, baseUserRepository, userStateService);
  }

  @Nested
  @DisplayName("Invalidate tokens for user by username")
  class InvalidateTokensForUserByUsername {

    @Test
    @DisplayName("Should successfully invalidate tokens for user")
    void successful() {
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> tokenInvalidationService.invalidateTokensForUserByUsername(TEST_USERNAME));

      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(sessionTokenRepository);
      verifyNoInteractions(baseUserRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when repository throws exception")
    void repositoryThrowsException() {
      RuntimeException repositoryException = new RuntimeException("Database error");
      doThrow(repositoryException)
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> tokenInvalidationService.invalidateTokensForUserByUsername(TEST_USERNAME));

      assertEquals("Failed to invalidate tokens for user " + TEST_USERNAME, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(sessionTokenRepository);
      verifyNoInteractions(baseUserRepository);
    }
  }

  @Nested
  @DisplayName("Invalidate tokens on user update")
  class InvalidateTokensOnUserUpdate {

    @Test
    @DisplayName("Should successfully invalidate tokens when relevant changes detected")
    void successfulWithRelevantChanges() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.GERMAN, true, true, "local");

      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(originalUser));
      doNothing().when(entityManager).clear();
      doNothing().when(entityManager).detach(originalUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensOnUserUpdate(currentUser));

      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
      verify(entityManager, times(1)).clear();
      verify(entityManager, times(1)).detach(originalUser);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should skip token invalidation when no relevant changes detected")
    void skipInvalidationNoRelevantChanges() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(originalUser));
      doNothing().when(entityManager).clear();
      doNothing().when(entityManager).detach(originalUser);

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensOnUserUpdate(currentUser));

      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
      verify(entityManager, times(1)).clear();
      verify(entityManager, times(1)).detach(originalUser);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should invalidate tokens when original user not found")
    void originalUserNotFound() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());
      doNothing().when(entityManager).clear();

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensOnUserUpdate(currentUser));

      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
      verify(entityManager, times(1)).clear();
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when token deletion fails")
    void tokenDeletionFails() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.GERMAN, true, true, "local");
      RuntimeException deletionException = new RuntimeException("Token deletion failed");

      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(originalUser));
      doNothing().when(entityManager).clear();
      doNothing().when(entityManager).detach(originalUser);
      doThrow(deletionException)
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> tokenInvalidationService.invalidateTokensOnUserUpdate(currentUser));

      assertEquals(
          "Failed to invalidate tokens for user mit ID " + TEST_USER_ID, exception.getMessage());
      assertEquals(deletionException, exception.getCause());
    }
  }

  @Nested
  @DisplayName("Invalidate tokens for role")
  class InvalidateTokensForRole {

    @Test
    @DisplayName("Should successfully invalidate tokens for all users with role")
    void successful() {
      List<String> usernames = List.of("user1@example.com", "user2@example.com");
      when(baseUserRepository.findAllUsernamesByRole(TEST_ROLE_NAME)).thenReturn(usernames);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(anyString());

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRole(TEST_ROLE_NAME);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should handle empty user list for role")
    void emptyUserList() {
      when(baseUserRepository.findAllUsernamesByRole(TEST_ROLE_NAME)).thenReturn(List.of());

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRole(TEST_ROLE_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when repository throws exception")
    void repositoryThrowsException() {
      RuntimeException repositoryException = new RuntimeException("Database error");
      when(baseUserRepository.findAllUsernamesByRole(TEST_ROLE_NAME))
          .thenThrow(repositoryException);

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> tokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

      assertEquals(
          "Failed to invalidate tokens for role " + TEST_ROLE_NAME, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(baseUserRepository, times(1)).findAllUsernamesByRole(TEST_ROLE_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }
  }

  @Nested
  @DisplayName("Invalidate tokens for right")
  class InvalidateTokensForRight {

    @Test
    @DisplayName("Should successfully invalidate tokens for all users with right")
    void successful() {
      List<String> usernames = List.of("user1@example.com", "user2@example.com");
      when(baseUserRepository.findAllUsernamesByRight(TEST_RIGHT_NAME)).thenReturn(usernames);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(anyString());

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRight(TEST_RIGHT_NAME);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should handle empty user list for right")
    void emptyUserList() {
      when(baseUserRepository.findAllUsernamesByRight(TEST_RIGHT_NAME)).thenReturn(List.of());

      assertDoesNotThrow(() -> tokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRight(TEST_RIGHT_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when repository throws exception")
    void repositoryThrowsException() {
      RuntimeException repositoryException = new RuntimeException("Database error");
      when(baseUserRepository.findAllUsernamesByRight(TEST_RIGHT_NAME))
          .thenThrow(repositoryException);

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> tokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

      assertEquals(
          "Failed to invalidate tokens for right " + TEST_RIGHT_NAME, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(baseUserRepository, times(1)).findAllUsernamesByRight(TEST_RIGHT_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }
  }

  @Nested
  @DisplayName("Fetch original user state")
  class FetchOriginalUserState {

    @Test
    @DisplayName("Should successfully fetch and detach original user")
    void successful() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.GERMAN, true, true, "local");

      // Mock EntityManager methods
      doNothing().when(entityManager).clear();
      doNothing().when(entityManager).detach(originalUser);

      // Mock repository to return the original user
      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(originalUser));

      Optional<AbstractBaseUser<?>> optionalAbstractBaseUser =
          userStateService.fetchOriginalUserState(currentUser);
      assertTrue(optionalAbstractBaseUser.isPresent());
      AbstractBaseUser<?> abstractBaseUser = optionalAbstractBaseUser.get();
      assertInstanceOf(UserStub.class, abstractBaseUser);
      UserStub result = (UserStub) abstractBaseUser;

      assertNotNull(result);
      assertEquals(originalUser, result);
      verify(entityManager, times(1)).clear();
      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
      verify(entityManager, times(1)).detach(originalUser);
    }

    @Test
    @DisplayName("Should return null when user not found")
    void userNotFound() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      // Mock EntityManager methods
      doNothing().when(entityManager).clear();

      // Mock repository to return empty Optional
      when(baseUserRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

      Optional<AbstractBaseUser<?>> optionalAbstractBaseUser =
          userStateService.fetchOriginalUserState(currentUser);
      assertTrue(optionalAbstractBaseUser.isPresent());
      AbstractBaseUser<?> abstractBaseUser = optionalAbstractBaseUser.get();
      assertInstanceOf(UserStub.class, abstractBaseUser);
      UserStub result = (UserStub) abstractBaseUser;

      assertNull(result);
      verify(entityManager, times(1)).clear();
      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
      verify(entityManager, never()).detach(any());
    }

    @Test
    @DisplayName("Should return null when exception occurs")
    void exceptionOccurs() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      // Mock EntityManager methods
      doNothing().when(entityManager).clear();

      // Mock repository to throw exception
      when(baseUserRepository.findById(TEST_USER_ID))
          .thenThrow(new RuntimeException("Database error"));

      Optional<AbstractBaseUser<?>> optionalAbstractBaseUser =
          userStateService.fetchOriginalUserState(currentUser);
      assertTrue(optionalAbstractBaseUser.isPresent());
      AbstractBaseUser<?> abstractBaseUser = optionalAbstractBaseUser.get();
      assertInstanceOf(UserStub.class, abstractBaseUser);
      UserStub result = (UserStub) abstractBaseUser;

      assertNull(result);
      verify(entityManager, times(1)).clear();
      verify(baseUserRepository, times(1)).findById(TEST_USER_ID);
    }
  }

  @Nested
  @DisplayName("Has relevant changes")
  class HasRelevantChanges {

    @Test
    @DisplayName("Should return true when email changed")
    void emailChanged() {
      UserStub originalUser =
          createMockUser("old@example.com", Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser("new@example.com", Locale.ENGLISH, true, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when locale changed")
    void localeChanged() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.GERMAN, true, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when enabled status changed")
    void enabledStatusChanged() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, false, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when account lock status changed")
    void accountLockStatusChanged() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, false, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when source changed")
    void sourceChanged() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "oauth");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when no relevant changes")
    void noRelevantChanges() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, currentUser);

      assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when original user is null")
    void originalUserNull() {
      UserStub currentUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(null, currentUser);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when current user is null")
    void currentUserNull() {
      UserStub originalUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      boolean result = tokenInvalidationService.hasRelevantChanges(originalUser, null);

      assertTrue(result);
    }
  }

  private UserStub createMockUser(
      String email, Locale locale, boolean enabled, boolean accountNonLocked, String source) {
    UserStub user = mock(UserStub.class);
    lenient().when(user.getId()).thenReturn(TEST_USER_ID);
    lenient().when(user.getEmail()).thenReturn(email);
    lenient().when(user.getUsername()).thenReturn(email);
    lenient().when(user.getLocale()).thenReturn(locale);
    lenient().when(user.isEnabled()).thenReturn(enabled);
    lenient().when(user.isAccountNonLocked()).thenReturn(accountNonLocked);
    lenient().when(user.getSource()).thenReturn(source);
    lenient().when(user.getRoles()).thenReturn(Set.of());
    return user;
  }
}
