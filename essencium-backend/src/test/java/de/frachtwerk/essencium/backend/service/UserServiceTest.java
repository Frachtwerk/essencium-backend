package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.api.assertions.EssenciumAssertions.assertThat;
import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.TEST_USER_ID;
import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.annotations.TestUserStub;
import de.frachtwerk.essencium.backend.api.annotations.TestUserStubType;
import de.frachtwerk.essencium.backend.api.annotations.UseTestObjects;
import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@UseTestObjects
@DisplayName("Interact with the UserService")
public class UserServiceTest {

  @Mock BaseUserRepository<UserStub, Long> userRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock JwtTokenService jwtTokenServiceMock;

  private UserServiceStub serviceStub;
  public final String TEST_MAIL = "test_user@example.com";
  private final String EXTERNAL_SOURCE = "ldap";
  private final String TEST_NONCE = "78fd553y";
  private final String TEST_PASSWORD = "testPassword";
  private final String TEST_ENCODED_PASSWORD = "BANANARAMA";

  @BeforeEach
  void setUp() {
    serviceStub =
        TestObjects.services()
            .defaultUserService(
                userRepositoryMock,
                passwordEncoderMock,
                userMailServiceMock,
                roleServiceMock,
                jwtTokenServiceMock);
  }

  @Nested
  @DisplayName("Fetch user objects by id")
  class GetUserById {

    @Test
    @DisplayName("Should return user if it is present in the repository")
    void userPresent() {
      var userStub = TestObjects.users().defaultUser();
      doReturn(Optional.of(userStub)).when(userRepositoryMock).findById(userStub.getId());

      var fetchedUserById = serviceStub.getById(userStub.getId());

      Assertions.assertThat(fetchedUserById).isSameAs(userStub);
    }

    @Test
    @DisplayName(
        "Should throw a ResourceNotFoundException if user is not present in the repository")
    void userNotFound() {
      assertThatThrownBy(() -> serviceStub.getById(123L))
          .isInstanceOf(ResourceNotFoundException.class);
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

      UserStub createdUser = serviceStub.createDefaultUser(userInfoEssentials, testSource);

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
          .and(configure(roleServiceMock).returnRoleOnGetByName(testRole));

      UserStub createdUser = serviceStub.create(userDto);

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
          TestObjects.users().userDtoBuilder().withPassword(TEST_PASSWORD).buildDefaultUserDto();

      givenMocks(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_ENCODED_PASSWORD, TEST_PASSWORD));

      UserStub createdUser = serviceStub.create(userDto);

      assertThat(createdUser).isNonNull().andHasPassword(TEST_ENCODED_PASSWORD);
      assertThat(userRepositoryMock).invokedSaveOneTime();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "}) // six numbers
    @DisplayName(
        "Should create a new random password if password is null / empty and request is local")
    void passwordNullEmptyOrBlank(String password) {
      UserDto<Long> userDto =
          TestObjects.users()
              .userDtoBuilder()
              .withPassword(password)
              .withEmail(TEST_MAIL)
              .buildDefaultUserDto();
      final AtomicReference<String> capturedPassword = new AtomicReference<>();

      givenMocks(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave())
          .and(configure(userMailServiceMock).trackNewUserMailSend())
          .and(
              configure(passwordEncoderMock)
                  .writePassedPasswordInAndReturn(capturedPassword, TEST_ENCODED_PASSWORD));

      UserStub createdUser = serviceStub.create(userDto);

      assertThat(createdUser)
          .isNonNull()
          .andHasAValidPasswordResetToken()
          .andHasPassword(TEST_ENCODED_PASSWORD);

      assertThat(userMailServiceMock).sendInTotalMails(1);
      assertThat(userMailServiceMock)
          .hasSentAMailTo(TEST_MAIL)
          .withParameter(createdUser.getPasswordResetToken());

      Assertions.assertThat(capturedPassword.get()).isNotBlank();
      Assertions.assertThat(capturedPassword.get().getBytes()).hasSizeGreaterThan(32);
      Assertions.assertThat(capturedPassword.get())
          .isNotEqualTo(createdUser.getPasswordResetToken());
    }

    @Test
    @DisplayName("Should not send a email if user creation source is external")
    void externalAuth() {
      UserDto<Long> userDto =
          TestObjects.users()
              .userDtoBuilder()
              .withEmail(TEST_MAIL)
              .withSource(EXTERNAL_SOURCE)
              .buildDefaultUserDto();

      givenMocks(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave());

      UserStub createdUser = serviceStub.create(userDto);

      assertThat(createdUser)
          .isNonNull()
          .andHasEmail(TEST_MAIL)
          .andHasSource(EXTERNAL_SOURCE)
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
      assertThatThrownBy(() -> serviceStub.update(userDto.getId() + 1, userDto))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    @DisplayName("Should throw a ResourceNotFoundException when updating a non existing user")
    void userNotFound(UserDto<Long> userDto) {
      assertThatThrownBy(() -> serviceStub.update(userDto.getId(), userDto))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update a users password successfully")
    void updateSuccessful(UserDto<Long> userToUpdateDto) {
      userToUpdateDto.setPassword(TEST_PASSWORD);

      givenMocks(
              configure(userRepositoryMock)
                  .returnOnFindByIdFor(userToUpdateDto.getId(), TestObjects.users().defaultUser())
                  .returnAlwaysPassedObjectOnSave())
          .and(configure(roleServiceMock).returnDefaultRoleOnDefaultRoleCall())
          .and(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_ENCODED_PASSWORD, TEST_PASSWORD));

      UserStub updatedUser = serviceStub.update(userToUpdateDto.getId(), userToUpdateDto);

      assertThat(updatedUser).isNonNull().andHasPassword(TEST_ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("Should not update a users password if the user is external")
    void testNoPasswordUpdateForExternalUser(
        UserDto<Long> userToUpdateDto,
        @TestUserStub(type = TestUserStubType.EXTERNAL) UserStub existingUser) {
      String newFirstName = "Tobi";

      userToUpdateDto.setPassword(TEST_PASSWORD);
      userToUpdateDto.setFirstName(newFirstName);

      givenMocks(
          configure(userRepositoryMock)
              .returnOnFindByIdFor(userToUpdateDto.getId(), existingUser)
              .returnAlwaysPassedObjectOnSave());

      UserStub updatedUser = serviceStub.update(userToUpdateDto.getId(), userToUpdateDto);

      assertThat(updatedUser)
          .isNonNull()
          .andHasNoPasswordNorPasswordResetToken()
          .andHasFirstName(newFirstName);
    }
  }

  @Nested
  @DisplayName("Update parts of User objects")
  class UpdateUserFields {

    private final Map<String, Object> PATCH_FIELDS = new HashMap<>();

    @BeforeEach
    void setUp() {
      PATCH_FIELDS.clear();
    }

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

      UserStub patchedUser = serviceStub.patch(TEST_USER_ID, PATCH_FIELDS);

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
      assertThatThrownBy(() -> serviceStub.patch(1L, PATCH_FIELDS))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw a ResourceUpdateException if patching with an unknown field")
    void unknownField(UserStub existingUser) {
      PATCH_FIELDS.put("UNKNOWN_FIELD", "Don´t care");

      givenMocks(configure(userRepositoryMock).returnOnFindByIdFor(TEST_USER_ID, existingUser));

      assertThatThrownBy(() -> serviceStub.patch(TEST_USER_ID, PATCH_FIELDS))
          .isInstanceOf(ResourceUpdateException.class);
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
      PATCH_FIELDS.put("password", TEST_PASSWORD);

      givenMocks(
              configure(passwordEncoderMock)
                  .returnEncodedPasswordWhenPasswordGiven(TEST_ENCODED_PASSWORD, TEST_PASSWORD))
          .and(
              configure(userRepositoryMock)
                  .returnOnFindByIdFor(TEST_USER_ID, existingUser)
                  .returnAlwaysPassedObjectOnSave());

      UserStub patchedUser = serviceStub.patch(TEST_USER_ID, PATCH_FIELDS);

      assertThat(patchedUser)
          .isNonNull()
          .andHasPassword(TEST_ENCODED_PASSWORD)
          .andHasFirstName(updateFirstName)
          .andHasLastName(updateLastName)
          .andHasPhone(updatePhone);
    }
  }
}
