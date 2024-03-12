package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.api.MockConfig.useMocking;
import static de.frachtwerk.essencium.backend.api.assertions.EssenciumAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Interact with the UserService")
public class UserServiceTest {

  @Mock BaseUserRepository<UserStub, Long> userRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock JwtTokenService jwtTokenServiceMock;

  private UserServiceStub serviceStub;

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

    private final String TEST_PASSWORD = "testPassword";
    private final String TEST_ENCODED_PASSWORD = "BANANARAMA";

    @Test
    @DisplayName("Should create a default user from UserInfoEssentials")
    void defaultUser() {
      final String testUsername = "Elon.Musk@frachtwerk.de";
      final String testSource = "Straight outta Compton";
      UserInfoEssentials userInfoEssentials =
          UserInfoEssentials.builder().username(testUsername).build();

      useMocking()
          .returnPassedEntityOnSaveFor(userRepositoryMock)
          .returnTestObjectsDefaultRoleOnDefaultRoleFor(roleServiceMock);

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
          TestObjects.users().userDtoBuilder().withRoles(testRole.getName()).buildLongUserDto();

      useMocking()
          .returnPassedEntityOnSaveFor(userRepositoryMock)
          .createAndReturnNewRoleOnEveryGetByNameFor(roleServiceMock);

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
          TestObjects.users().userDtoBuilder().withPassword(TEST_PASSWORD).buildLongUserDto();

      useMocking()
          .returnPassedEntityOnSaveFor(userRepositoryMock)
          .returnEncodedPasswordFor(passwordEncoderMock, TEST_PASSWORD, TEST_ENCODED_PASSWORD);

      UserStub createdUser = serviceStub.create(userDto);

      assertThat(createdUser).isNonNull().andHasPassword(TEST_ENCODED_PASSWORD);
      assertThat(userRepositoryMock).invokedSaveOneTime();
    }
  }
}
