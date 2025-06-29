package de.frachtwerk.essencium.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.user.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTokenInvalidationService Tests")
class TokenInvalidationUuidServiceTest {

  @Mock SessionTokenRepository sessionTokenRepository;
  @Mock BaseUserRepository<TestUUIDUser, UUID> baseUserRepository;

  @InjectMocks SessionTokenInvalidationService sessionTokenInvalidationService;

  private static final String TEST_USERNAME = "test@example.com";
  private static final String TEST_ROLE_NAME = "ADMIN";
  private static final String TEST_RIGHT_NAME = "READ_USERS";
  private static final UUID TEST_USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    // Any additional setup if needed
  }

  @Nested
  @DisplayName("Invalidate tokens for user by username")
  class InvalidateTokensForUserByUsername {

    @Test
    @DisplayName("Should successfully invalidate tokens for user")
    void successful() {
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensForUserByUsername(TEST_USERNAME));

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
              () ->
                  sessionTokenInvalidationService.invalidateTokensForUserByUsername(TEST_USERNAME));

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
    @DisplayName("Should invalidate tokens when email changed")
    void emailChanged() {
      TestUUIDUser existingUser =
          createMockUser("old@example.com", Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser =
          createMockUser("new@example.com", Locale.ENGLISH, true, true, "local");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing()
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("old@example.com");

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("old@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should invalidate tokens when locale changed")
    void localeChanged() {
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.GERMAN, true, true, "local");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should invalidate tokens when roles changed")
    void rolesChanged() {
      Role role1 = mock(Role.class);
      Role role2 = mock(Role.class);
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      when(existingUser.getRoles()).thenReturn(Set.of(role1));
      when(updatedUser.getRoles()).thenReturn(Set.of(role2));

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should invalidate tokens when enabled status changed")
    void enabledStatusChanged() {
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, false, true, "local");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should invalidate tokens when account lock status changed")
    void accountLockStatusChanged() {
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, false, "local");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should invalidate tokens when source changed")
    void sourceChanged() {
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "ldap");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doNothing().when(sessionTokenRepository).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1)).deleteAllByUsernameEqualsIgnoreCase(TEST_USERNAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should not invalidate tokens when no relevant fields changed")
    void noRelevantFieldsChanged() {
      TestUUIDUser existingUser =
          createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw NullPointerException when user ID is null")
    void userIdIsNull() {
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      when(updatedUser.getId()).thenReturn(null);

      assertThrows(
          TokenInvalidationException.class,
          () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      verifyNoInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName(
        "Should throw TokenInvalidationException when repository throws EntityNotFoundException")
    void entityNotFoundException() {
      TestUUIDUser updatedUser = createMockUser(TEST_USERNAME, Locale.ENGLISH, true, true, "local");
      EntityNotFoundException repositoryException = new EntityNotFoundException("User not found");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenThrow(repositoryException);

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      assertEquals(
          "Failed to invalidate tokens for user mit ID " + TEST_USER_ID, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when token deletion fails")
    void tokenDeletionFails() {
      TestUUIDUser existingUser =
          createMockUser("old@example.com", Locale.ENGLISH, true, true, "local");
      TestUUIDUser updatedUser =
          createMockUser("new@example.com", Locale.ENGLISH, true, true, "local");
      RuntimeException deletionException = new RuntimeException("Token deletion failed");

      when(baseUserRepository.getReferenceById(TEST_USER_ID)).thenReturn(existingUser);
      doThrow(deletionException)
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("old@example.com");

      TokenInvalidationException exception =
          assertThrows(
              TokenInvalidationException.class,
              () -> sessionTokenInvalidationService.invalidateTokensOnUserUpdate(updatedUser));

      assertEquals(
          "Failed to invalidate tokens for user mit ID " + TEST_USER_ID, exception.getMessage());
      assertEquals(deletionException, exception.getCause());
      verify(baseUserRepository, times(1)).getReferenceById(TEST_USER_ID);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("old@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
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

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

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

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

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
              () -> sessionTokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

      assertEquals(
          "Failed to invalidate tokens for role " + TEST_ROLE_NAME, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(baseUserRepository, times(1)).findAllUsernamesByRole(TEST_ROLE_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when token deletion fails for one user")
    void tokenDeletionFailsForOneUser() {
      List<String> usernames = List.of("user1@example.com", "user2@example.com");
      RuntimeException deletionException = new RuntimeException("Token deletion failed");

      when(baseUserRepository.findAllUsernamesByRole(TEST_ROLE_NAME)).thenReturn(usernames);
      doNothing()
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      doThrow(deletionException)
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");

      assertThrows(
          TokenInvalidationException.class,
          () -> sessionTokenInvalidationService.invalidateTokensForRole(TEST_ROLE_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRole(TEST_ROLE_NAME);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
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

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

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

      assertDoesNotThrow(
          () -> sessionTokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

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
              () -> sessionTokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

      assertEquals(
          "Failed to invalidate tokens for right " + TEST_RIGHT_NAME, exception.getMessage());
      assertEquals(repositoryException, exception.getCause());
      verify(baseUserRepository, times(1)).findAllUsernamesByRight(TEST_RIGHT_NAME);
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoInteractions(sessionTokenRepository);
    }

    @Test
    @DisplayName("Should throw TokenInvalidationException when token deletion fails for one user")
    void tokenDeletionFailsForOneUser() {
      List<String> usernames = List.of("user1@example.com", "user2@example.com");
      RuntimeException deletionException = new RuntimeException("Token deletion failed");

      when(baseUserRepository.findAllUsernamesByRight(TEST_RIGHT_NAME)).thenReturn(usernames);
      doNothing()
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      doThrow(deletionException)
          .when(sessionTokenRepository)
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");

      assertThrows(
          TokenInvalidationException.class,
          () -> sessionTokenInvalidationService.invalidateTokensForRight(TEST_RIGHT_NAME));

      verify(baseUserRepository, times(1)).findAllUsernamesByRight(TEST_RIGHT_NAME);
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user1@example.com");
      verify(sessionTokenRepository, times(1))
          .deleteAllByUsernameEqualsIgnoreCase("user2@example.com");
      verifyNoMoreInteractions(baseUserRepository);
      verifyNoMoreInteractions(sessionTokenRepository);
    }
  }

  private TestUUIDUser createMockUser(
      String email, Locale locale, boolean enabled, boolean accountNonLocked, String source) {
    TestUUIDUser user = mock(TestUUIDUser.class);
    lenient().when(user.getId()).thenReturn(TEST_USER_ID);
    lenient().when(user.getEmail()).thenReturn(email);
    lenient().when(user.getLocale()).thenReturn(locale);
    lenient().when(user.isEnabled()).thenReturn(enabled);
    lenient().when(user.isAccountNonLocked()).thenReturn(accountNonLocked);
    lenient().when(user.getSource()).thenReturn(source);
    lenient().when(user.getRoles()).thenReturn(Set.of());
    return user;
  }
}
