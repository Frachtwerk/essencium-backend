package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.api.assertions.EssenciumAssertions.assertThat;
import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.*;
import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.annotations.*;
import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@ExtendWith(MockitoExtension.class)
@UseTestObjects
@DisplayName("Interact with the UserService")
public class UserServiceTest {

  @Mock BaseUserRepository<UserStub, Long> userRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock JwtTokenService jwtTokenServiceMock;

  private UserServiceStub testSubject;

  private final String NEW_PASSWORD_PLAIN = "secret password!";
  private final String NEW_PASSWORD_HASH = "{hash} secret password";
  private final Map<String, Object> PATCH_FIELDS = new HashMap<>();

  @BeforeEach
  void setUp() {
    testSubject =
        TestObjects.services()
            .defaultUserService(
                userRepositoryMock,
                passwordEncoderMock,
                userMailServiceMock,
                roleServiceMock,
                jwtTokenServiceMock);

    PATCH_FIELDS.clear();
  }

  @Nested
  @DisplayName("Fetch user objects by id")
  class GetUserById {

    @Test
    @DisplayName("Should return user if it is present in the repository")
    void userPresent() {
      var userStub = TestObjects.users().internal();
      doReturn(Optional.of(userStub)).when(userRepositoryMock).findById(userStub.getId());

      var fetchedUserById = testSubject.getById(userStub.getId());

      Assertions.assertThat(fetchedUserById).isSameAs(userStub);
    }

    @Test
    @DisplayName(
        "Should throw a ResourceNotFoundException if user is not present in the repository")
    void userNotFound() {
      assertThrows(ResourceNotFoundException.class, () -> testSubject.getById(123L));
    }
  }

  @Nested
  @DisplayName("Create user objects")
  class CreateUser {
    @Test
    @DisplayName("Should create a default user from UserInfoEssentials")
    void defaultUser() {
      final String testUsername = "Elon.Musk@frachtwerk.de";
      final String testSource = "Straight outta Compton";
      UserInfoEssentials userInfoEssentials =
          UserInfoEssentials.builder().username(testUsername).build();

      givenMocks(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall());

      UserStub createdUser = testSubject.createDefaultUser(userInfoEssentials, testSource);

      assertThat(createdUser)
          .isNonNull()
          .andHasEmail(testUsername.toLowerCase())
          .andHasSource(testSource)
          .andHasOnlyTheRoles(TestObjects.roles().defaultRole());
      assertThat(userRepositoryMock).invokedSaveOneTime();
    }

    @Test
    @DisplayName("Should create a user with a custom role from a UserDTO")
    void customRole() {
      Role testRole = TestObjects.roles().roleWithNameAndDescription("SPECIAL_ROLE");
      UserDto<Long> userDto =
          TestObjects.users().userDtoBuilder().withRoles(testRole.getName()).buildDefaultUserDto();

      givenMocks(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(configure(roleServiceMock).returnRoleOnGetByNameFor(testRole));

      UserStub createdUser = testSubject.create(userDto);

      assertThat(createdUser)
          .isNonNull()
          .andHasEmail(userDto.getEmail())
          .andHasOnlyTheRoles(testRole);
      assertThat(userRepositoryMock).invokedSaveOneTime();
    }

    @Test
    @DisplayName("Should create a user with password, where only the encoded String is present")
    void passwordPresent() {
      UserDto<Long> userDto =
          TestObjects.users()
              .userDtoBuilder()
              .withPassword(TEST_PASSWORD_PLAIN)
              .buildDefaultUserDto();

      givenMocks(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_PASSWORD_HASH, TEST_PASSWORD_PLAIN));

      UserStub createdUser = testSubject.create(userDto);

      assertThat(createdUser).isNonNull().andHasPassword(TEST_PASSWORD_HASH);
      assertThat(userRepositoryMock).invokedSaveOneTime();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""}) // six numbers
    @DisplayName(
        "Should create a new random password if password is null / empty and request is local")
    void passwordNullEmptyOrBlank(String password) {
      UserDto<Long> userDto =
          TestObjects.users()
              .userDtoBuilder()
              .withPassword(password)
              .withEmail(TEST_USERNAME)
              .buildDefaultUserDto();
      final AtomicReference<String> capturedPassword = new AtomicReference<>();

      givenMocks(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(configure(userMailServiceMock).trackNewUserMailSend())
          .and(
              configure(passwordEncoderMock)
                  .writePassedPasswordInAndReturn(capturedPassword, TEST_PASSWORD_HASH));

      UserStub createdUser = testSubject.create(userDto);

      assertThat(createdUser)
          .isNonNull()
          .andHasAValidPasswordResetToken()
          .andHasPassword(TEST_PASSWORD_HASH);

      assertThat(userMailServiceMock).sendInTotalMails(1);
      assertThat(userMailServiceMock)
          .hasSentAMailTo(TEST_USERNAME)
          .withParameter(createdUser.getPasswordResetToken());

      Assertions.assertThat(capturedPassword.get()).isNotBlank();
      Assertions.assertThat(capturedPassword.get().getBytes()).hasSizeGreaterThan(32);
      Assertions.assertThat(capturedPassword.get())
          .isNotEqualTo(createdUser.getPasswordResetToken());
    }

    @Test
    @DisplayName("Should not send a email if user creation source is external")
    void externalAuth() {
      String NEW_EXTERNAL_SOURCE = "ldap";
      UserDto<Long> userDto =
          TestObjects.users()
              .userDtoBuilder()
              .withEmail(TEST_USERNAME)
              .withSource(NEW_EXTERNAL_SOURCE)
              .buildDefaultUserDto();

      givenMocks(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave());

      UserStub createdUser = testSubject.create(userDto);

      assertThat(createdUser)
          .isNonNull()
          .andHasEmail(TEST_USERNAME)
          .andHasSource(NEW_EXTERNAL_SOURCE)
          .andHasNoPasswordNorPasswordResetToken()
          .andHasOnlyTheRoles(TestObjects.roles().defaultRole());

      assertThat(userRepositoryMock).invokedSaveOneTime();
      assertThat(userMailServiceMock).hasSendNoMails();
    }
  }

  @Nested
  @DisplayName("Update full User objects")
  class UpdateUser {

    @Test
    @DisplayName("Should throw a ResourceUpdateException when updating with an inconsistent ID")
    void inconsistentId(UserDto<Long> userDto) {
      assertThrows(
          ResourceUpdateException.class, () -> testSubject.update(userDto.getId() + 1, userDto));
    }

    @Test
    @DisplayName("Should throw a ResourceNotFoundException when updating a non existing user")
    void userNotFound(UserDto<Long> userDto) {
      assertThrows(
          ResourceNotFoundException.class, () -> testSubject.update(userDto.getId(), userDto));
    }

    @Test
    @DisplayName("Should update a users password successfully")
    void updateSuccessful(UserDto<Long> userToUpdateDto) {
      userToUpdateDto.setPassword(TEST_PASSWORD_PLAIN);

      givenMocks(
              configure(userRepositoryMock)
                  .returnOnFindByIdFor(userToUpdateDto.getId(), TestObjects.users().internal())
                  .returnAlwaysPassedObjectOnSave())
          .and(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_PASSWORD_HASH, TEST_PASSWORD_PLAIN));

      UserStub updatedUser = testSubject.update(userToUpdateDto.getId(), userToUpdateDto);

      assertThat(updatedUser).isNonNull().andHasPassword(TEST_PASSWORD_HASH);
    }

    @Test
    @DisplayName("Should not update a users password if the user is external")
    void testNoPasswordUpdateForExternalUser(
        UserDto<Long> userToUpdateDto,
        @TestUserStub(type = TestUserStubType.EXTERNAL) UserStub existingUser) {
      String newFirstName = "Tobi";

      userToUpdateDto.setPassword(TEST_PASSWORD_PLAIN);
      userToUpdateDto.setFirstName(newFirstName);

      givenMocks(
          configure(userRepositoryMock)
              .returnOnFindByIdFor(userToUpdateDto.getId(), existingUser)
              .returnAlwaysPassedObjectOnSave());

      UserStub updatedUser = testSubject.update(userToUpdateDto.getId(), userToUpdateDto);

      assertThat(updatedUser)
          .isNonNull()
          .andHasNoPasswordNorPasswordResetToken()
          .andHasFirstName(newFirstName);
    }
  }

  @Nested
  @DisplayName("Update parts of User objects")
  class UpdateUserFields {
    @Test
    @DisplayName("Should not patch a password for a external user")
    void testNoPasswordPatchForExternalUser(
        @TestUserStub(type = TestUserStubType.EXTERNAL) UserStub existingUser) {
      final String NEW_FIRST_NAME = "Tobi";
      final String CURRENT_PASSWORD = existingUser.getPassword();

      PATCH_FIELDS.putAll(
          Map.of("id", TEST_USER_ID, "firstName", NEW_FIRST_NAME, "password", "shouldbeignored"));

      givenMocks(
          configure(userRepositoryMock)
              .returnOnFindByIdFor(TEST_USER_ID, existingUser)
              .returnAlwaysPassedObjectOnSave());

      UserStub patchedUser = testSubject.patch(TEST_USER_ID, PATCH_FIELDS);

      assertThat(patchedUser)
          .isNonNull()
          .andHasNonce(TEST_NONCE)
          .andHasId(TEST_USER_ID)
          .andHasNoPasswordNorPasswordResetToken();

      assertThat(userRepositoryMock).invokedSaveNTimes(2);
      assertThat(userRepositoryMock).invokedFindByIdNTimes(2);
      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }

    @Test
    @DisplayName("Should throw a ResourceNotFoundException if patching a non existing user")
    void userNotFound() {
      assertThrows(ResourceNotFoundException.class, () -> testSubject.patch(1L, PATCH_FIELDS));
    }

    @Test
    @DisplayName("Should throw a ResourceUpdateException if patching with an unknown field")
    void unknownField(UserStub existingUser) {
      PATCH_FIELDS.put("UNKNOWN_FIELD", "Don´t care");

      givenMocks(configure(userRepositoryMock).returnOnFindByIdFor(TEST_USER_ID, existingUser));

      assertThrows(
          ResourceUpdateException.class, () -> testSubject.patch(TEST_USER_ID, PATCH_FIELDS));
    }

    @Test
    @DisplayName("Should patch a user successfully")
    void updateSuccessful(UserStub existingUser) {
      String updateFirstName = "Peter";
      String updateLastName = "Zwegat";
      String updatePhone = "555-1337424711";

      PATCH_FIELDS.put("firstName", updateFirstName);
      PATCH_FIELDS.put("lastName", updateLastName);
      PATCH_FIELDS.put("phone", updatePhone);
      PATCH_FIELDS.put("password", TEST_PASSWORD_PLAIN);

      givenMocks(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_PASSWORD_HASH, TEST_PASSWORD_PLAIN))
          .and(
              configure(userRepositoryMock)
                  .returnOnFindByIdFor(TEST_USER_ID, existingUser)
                  .returnAlwaysPassedObjectOnSave());

      UserStub patchedUser = testSubject.patch(TEST_USER_ID, PATCH_FIELDS);

      assertThat(patchedUser)
          .isNonNull()
          .andHasPassword(TEST_PASSWORD_HASH)
          .andHasFirstName(updateFirstName)
          .andHasLastName(updateLastName)
          .andHasPhone(updatePhone);
    }

    @Test
    @DisplayName("Should add Roles by name successfully")
    void updateAddRolesByNameSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role adminRole,
        @TestRole(name = "USER") Role userRole) {
      List<String> testRoleAuthorities = List.of(adminRole.getName(), userRole.getName());

      PATCH_FIELDS.put("roles", testRoleAuthorities);

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(
              configure(roleServiceMock)
                  .returnRoleOnGetByNameFor(adminRole)
                  .returnRoleOnGetByNameFor(userRole));

      Assertions.assertThat(existingUser.getRoles()).isEmpty();

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(adminRole, userRole);
    }

    @Test
    @DisplayName("Should remove Roles by name successfully")
    void updateRemoveRolesByNameSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role roleToKeep,
        @TestRole(name = "USER") Role roleToRemove) {
      List<String> testRolesToKeep = List.of(roleToKeep.getName());
      PATCH_FIELDS.put("roles", testRolesToKeep);

      existingUser.getRoles().addAll(List.of(roleToKeep, roleToRemove));

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(configure(roleServiceMock).returnRoleOnGetByNameFor(roleToKeep));

      Assertions.assertThat(existingUser.getRoles()).hasSize(2);

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(roleToKeep);
      assertThat(roleServiceMock).invokedNeverGetByNameFor(roleToRemove.getName());
    }

    @Test
    @DisplayName("Should add Roles by Role object successfully")
    void updateAddRolesByRoleSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role adminRole,
        @TestRole(name = "USER") Role userRole) {
      List<Role> testRoles = List.of(adminRole, userRole);

      PATCH_FIELDS.put("roles", testRoles);

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(
              configure(roleServiceMock)
                  .returnRoleOnGetByNameFor(adminRole)
                  .returnRoleOnGetByNameFor(userRole));

      Assertions.assertThat(existingUser.getRoles()).isEmpty();

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(adminRole, userRole);
    }

    @Test
    @DisplayName("Should remove Roles by Role object successfully")
    void updateRemoveRolesByRoleSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role roleToKeep,
        @TestRole(name = "USER") Role roleToRemove) {
      List<Role> testRolesToKeep = List.of(roleToKeep);
      PATCH_FIELDS.put("roles", testRolesToKeep);

      existingUser.getRoles().addAll(List.of(roleToKeep, roleToRemove));

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(configure(roleServiceMock).returnRoleOnGetByNameFor(roleToKeep));

      Assertions.assertThat(existingUser.getRoles()).hasSize(2);

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(roleToKeep);
      assertThat(roleServiceMock).invokedNeverGetByNameFor(roleToRemove.getName());
    }

    @Test
    @DisplayName("Should add Roles by Map successfully")
    void updateAddRolesByMapSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role adminRole,
        @TestRole(name = "USER") Role userRole) {
      List<Map<String, String>> adminRoleAuthorities =
          List.of(Map.of("name", adminRole.getName()), Map.of("name", userRole.getName()));

      PATCH_FIELDS.put("roles", adminRoleAuthorities);

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(
              configure(roleServiceMock)
                  .returnRoleOnGetByNameFor(adminRole)
                  .returnRoleOnGetByNameFor(userRole));

      Assertions.assertThat(existingUser.getRoles()).isEmpty();

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(adminRole, userRole);
    }

    @Test
    @DisplayName("Should remove Roles by Map successfully")
    void updateRemoveRolesByMapSuccessful(
        UserStub existingUser,
        @TestRole(name = "ADMIN") Role roleToKeep,
        @TestRole(name = "USER") Role roleToRemove) {
      List<Map<String, String>> testRolesToKeep = List.of(Map.of("name", roleToKeep.getName()));
      PATCH_FIELDS.put("roles", testRolesToKeep);

      existingUser.getRoles().addAll(List.of(roleToKeep, roleToRemove));

      givenMocks(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser))
          .and(configure(roleServiceMock).returnRoleOnGetByNameFor(roleToKeep));

      Assertions.assertThat(existingUser.getRoles()).hasSize(2);

      UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

      assertThat(patchedUser).isNonNull().andHasOnlyTheRoles(roleToKeep);
      assertThat(roleServiceMock).invokedNeverGetByNameFor(roleToRemove.getName());
    }
  }

  @Test
  @DisplayName("Should remove all given roles")
  void updateRemoveAllRolesSuccessful(
      UserStub existingUser,
      @TestRole(name = "ADMIN") Role roleToRemove,
      @TestRole(name = "USER") Role anotherRoleToRemove) {
    PATCH_FIELDS.put("roles", Collections.emptyList());

    existingUser.getRoles().addAll(List.of(anotherRoleToRemove, roleToRemove));

    givenMocks(
        configure(userRepositoryMock)
            .returnAlwaysPassedObjectOnSave()
            .returnOnFindByIdFor(existingUser.getId(), existingUser));

    Assertions.assertThat(existingUser.getRoles()).hasSize(2);

    UserStub patchedUser = testSubject.patch(existingUser.getId(), PATCH_FIELDS);

    assertThat(patchedUser).isNonNull().andHasNoRoles();
    assertThat(roleServiceMock).invokedNeverGetByNameFor(roleToRemove.getName());
    assertThat(roleServiceMock).invokedNeverGetByNameFor(anotherRoleToRemove.getName());
  }

  @Nested
  @DisplayName("Fetch the current login user")
  class GetCurrentLoggedInUser {
    @Test
    @DisplayName(
        "Should throw a SessionAuthenticationException when trying to get a user from non existing principal")
    void noUserLoggedIn() {
      assertThrows(
          SessionAuthenticationException.class, () -> testSubject.getUserFromPrincipal(null));
    }

    @Test
    @DisplayName(
        "Should throw a SessionAuthenticationException when no User is present in the principal")
    void userWronglyLoggedIn() {
      var wrongPrincipal = TestObjects.authentication().notLoggedInPrincipal();

      assertThrows(
          SessionAuthenticationException.class,
          () -> testSubject.getUserFromPrincipal(wrongPrincipal));
    }

    @Test
    @DisplayName("Should fetch the User from a logged in principal")
    void userIsLoggedIn(
        UserStub defaultUser, UsernamePasswordAuthenticationToken loggedInPrincipal) {
      UserStub loggedInUser = testSubject.getUserFromPrincipal(loggedInPrincipal);

      Assertions.assertThat(loggedInUser).isEqualTo(defaultUser);
    }
  }

  @Nested
  @DisplayName("Fetch user by username")
  class LoadUserByUsername {
    @Test
    @DisplayName("Should find an existing User by username")
    void userPresent(UserStub existingUser) {
      givenMocks(
          configure(userRepositoryMock)
              .returnUserForGivenEmailIgnoreCase(existingUser.getUsername(), existingUser));

      UserStub foundUser = testSubject.loadUserByUsername(existingUser.getUsername());

      Assertions.assertThat(foundUser).isEqualTo(existingUser);
    }

    @Test
    @DisplayName("Should throw a UsernameNotFoundException if no user can be found by username")
    void userNotFound() {
      givenMocks(configure(userRepositoryMock).returnNoUserForGivenEmailIgnoreCase(TEST_USERNAME));

      assertThrows(
          UsernameNotFoundException.class, () -> testSubject.loadUserByUsername(TEST_USERNAME));
    }
  }

  @Nested
  @DisplayName("Delete Users")
  class DeleteUsers {

    @Test
    @DisplayName("Should delete an existing Users by Id")
    void deleteUserById(UserStub existingUser) {
      givenMocks(
          configure(userRepositoryMock)
              .entityWithIdExists(existingUser.getId())
              .doNothingOnDeleteEntityWithId(existingUser.getId()));

      testSubject.deleteById(existingUser.getId());

      assertThat(userRepositoryMock).invokedDeleteByIdOneTime(existingUser.getId());
    }
  }

  @Nested
  @DisplayName("Create password reset tokens and reset password")
  class ResetPasswordWithToken {

    @Test
    @DisplayName("Should create a password reset token for an existing user")
    void successful(UserStub existingUser) {
      givenMocks(
              configure(userRepositoryMock)
                  .returnUserForGivenEmailIgnoreCase(existingUser.getEmail(), existingUser)
                  .returnAlwaysPassedObjectOnSave())
          .and(configure(userMailServiceMock).trackResetTokenSend());

      testSubject.createResetPasswordToken(existingUser.getUsername());

      assertThat(existingUser).isNonNull().andHasAValidPasswordResetToken();
      assertThat(userMailServiceMock)
          .hasSentAMailTo(existingUser.getEmail())
          .withParameter(existingUser.getPasswordResetToken());
    }

    @Test
    @DisplayName(
        "Should throw a NotAllowedException when creating a reset token for an external user")
    void createFailForExternalUser(
        @TestUserStub(type = TestUserStubType.EXTERNAL) UserStub externalUser) {
      givenMocks(
          configure(userRepositoryMock)
              .returnUserForGivenEmailIgnoreCase(externalUser.getEmail(), externalUser));

      assertThrows(
          NotAllowedException.class,
          () -> testSubject.createResetPasswordToken(externalUser.getUsername()));

      assertThat(externalUser).isNonNull().andHasNoPasswordNorPasswordResetToken();
      assertThat(userMailServiceMock).hasSendNoMails();
    }

    @Test
    @DisplayName("Should reset a password with token")
    void resetPasswordWithToken(
        @TestUserStub(type = TestUserStubType.PASSWORD_RESET) UserStub passwordResetUser) {
      givenMocks(
              configure(userRepositoryMock)
                  .returnUserForGivenPasswordResetToken(
                      passwordResetUser.getPasswordResetToken(), passwordResetUser)
                  .returnAlwaysPassedObjectOnSave())
          .and(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(NEW_PASSWORD_HASH, NEW_PASSWORD_PLAIN));

      assertThat(passwordResetUser).isNonNull().andHasAValidPasswordResetToken();

      testSubject.resetPasswordByToken(
          passwordResetUser.getPasswordResetToken(), NEW_PASSWORD_PLAIN);

      assertThat(passwordResetUser)
          .isNonNull()
          .andHasPassword(NEW_PASSWORD_HASH)
          .andHasNoPasswordResetToken()
          .andCanLogin();
    }

    @Test
    @DisplayName(
        "Should throw a BadCredentialsException when resetting password with an invalid token")
    void failResetPasswordWithInvalidToken(
        @TestUserStub(type = TestUserStubType.PASSWORD_RESET) UserStub passwordResetUser) {
      final String invalidToken = "invalid";
      final String preUpdatePassword = passwordResetUser.getPassword();

      assertThrows(
          BadCredentialsException.class,
          () -> testSubject.resetPasswordByToken(invalidToken, NEW_PASSWORD_HASH));

      assertThat(passwordResetUser).isNonNull().andHasPassword(preUpdatePassword);

      assertThat(userRepositoryMock).invokedSaveNTimes(0);
    }

    @Test
    @DisplayName("Should throw a UsernameNotFoundException if user not exists")
    void userNotFound() {
      assertThrows(
          UsernameNotFoundException.class,
          () -> testSubject.createResetPasswordToken(TEST_USERNAME));

      assertThat(userRepositoryMock).invokedFindByEmailIgnoreCaseOneTimeFor(TEST_USERNAME);
      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("Self update the current logged in User")
  class SelfUpdate {

    @Test
    @DisplayName("Should throw a RuntimeException if self update a null user")
    void testExceptionOnNullUser(UserDto<Long> userDto) {
      assertThrows(RuntimeException.class, () -> testSubject.selfUpdate(null, userDto));
    }

    @Test
    @DisplayName("Should self update the logged in User by DTO")
    void testUpdateUserByDto(
        UserStub existingUser, UsernamePasswordAuthenticationToken loggedInPrincipal) {
      UserDto<Long> updateDto = TestObjects.users().defaultUserUpdateDto();

      givenMocks(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave());

      final UserStub updatedSelf =
          testSubject.selfUpdate((UserStub) loggedInPrincipal.getPrincipal(), updateDto);

      assertThat(updatedSelf)
          .isNonNull()
          .andHasFirstName(updateDto.getFirstName())
          .andHasLastName(updateDto.getLastName())
          .andHasPhone(updateDto.getPhone())
          .andHasMobile(updateDto.getMobile())
          .andHasLocale(updateDto.getLocale())
          .andHasPassword(existingUser.getPassword());

      assertThat(userRepositoryMock).invokedSaveOneTime();
      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }

    @Test
    @DisplayName("Should self update the logged in User by fields")
    void testUpdateUserByFields(
        UserStub existingUser, UsernamePasswordAuthenticationToken loggedInPrincipal) {
      UserDto<Long> updateDto = TestObjects.users().defaultUserUpdateDto();

      PATCH_FIELDS.putAll(
          Map.of(
              "firstName", updateDto.getFirstName(),
              "lastName", updateDto.getLastName(),
              "phone", updateDto.getPhone(),
              "mobile", updateDto.getMobile(),
              "locale", updateDto.getLocale(),
              "password", updateDto.getPassword()));

      givenMocks(
          configure(userRepositoryMock)
              .returnAlwaysPassedObjectOnSave()
              .returnOnFindByIdFor(existingUser.getId(), existingUser));

      final UserStub updatedSelf =
          testSubject.selfUpdate((UserStub) loggedInPrincipal.getPrincipal(), PATCH_FIELDS);

      assertThat(updatedSelf)
          .isNonNull()
          .andHasFirstName(updateDto.getFirstName())
          .andHasLastName(updateDto.getLastName())
          .andHasPhone(updateDto.getPhone())
          .andHasMobile(updateDto.getMobile())
          .andHasLocale(updateDto.getLocale())
          .andHasPassword(existingUser.getPassword());

      assertThat(userRepositoryMock).invokedSaveNTimes(2);
      assertThat(userRepositoryMock).invokedFindByIdNTimes(2);
      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }

    @Test
    @DisplayName(
        "Should throw a BadCredentialsException if try to update a password with wrong credentials")
    void testUpdatePasswordWrongCredentials(UsernamePasswordAuthenticationToken loggedInPrincipal) {
      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest(NEW_PASSWORD_PLAIN, "wrong password");
      assertThrows(
          BadCredentialsException.class,
          () ->
              testSubject.updatePassword(
                  (UserStub) loggedInPrincipal.getPrincipal(), updateRequest));

      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }

    @Test
    @DisplayName("Should successfully update the password of the current logged in user")
    void testUpdatePasswordSuccess(
        UsernamePasswordAuthenticationToken loggedInPrincipal, UserStub existingUser) {
      final String NEW_PASSWORD_PLAIN = "secret password!";
      final String NEW_PASSWORD_HASH =
          "$2b$10$9yPzo9U15R1jqm6Db6Jg4uZTlvedRNii/orl4bQfY.nUBM5hL0/9a";

      givenMocks(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(NEW_PASSWORD_HASH, NEW_PASSWORD_PLAIN)
                  .passGivenPassword(TEST_PASSWORD_PLAIN, existingUser.getPassword()))
          .and(
              configure(userRepositoryMock)
                  .returnAlwaysPassedObjectOnSave()
                  .returnOnFindByIdFor(existingUser.getId(), existingUser));

      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest(NEW_PASSWORD_PLAIN, TEST_PASSWORD_PLAIN);

      final UserStub updatedSelf =
          testSubject.updatePassword((UserStub) loggedInPrincipal.getPrincipal(), updateRequest);

      assertThat(updatedSelf)
          .isNonNull()
          .andHasId(existingUser.getId())
          .andHasPassword(NEW_PASSWORD_HASH)
          .andHasNotNonce(existingUser.getNonce());

      assertThat(passwordEncoderMock).passwordUpdateMethodsAreTriggeredOnes();
      assertThat(userRepositoryMock).invokedSaveOneTime();
      assertThat(userRepositoryMock).invokedFindByIdNTimes(1);
      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }

    @Test
    @DisplayName("Should throw a NotAllowedException if updating a password of an existing user")
    void testUpdatePasswordFailForExternalUser(
        @TestPrincipal(type = TestPrincipleType.EXTERNAL_LOGGED_IN)
            UsernamePasswordAuthenticationToken externalPrincipal) {
      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest("shouldbeignored", TEST_PASSWORD_PLAIN);

      assertThrows(
          NotAllowedException.class,
          () ->
              testSubject.updatePassword(
                  (UserStub) externalPrincipal.getPrincipal(), updateRequest));

      assertThat(userRepositoryMock).hasNoMoreInteractions();
    }
  }

  @Test
  @DisplayName("Fetch all users as page")
  void getAll(Page<?> page, Pageable pageable) {
    doReturn(page).when(page).map(any());
    doReturn(page).when(userRepositoryMock).findAll(pageable);

    Page<UserStub> all = testSubject.getAll(pageable);

    Assertions.assertThat(all).isEqualTo(page);
  }
}
