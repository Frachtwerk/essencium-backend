/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenUserDto;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.*;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenUserRepresentation;
import de.frachtwerk.essencium.backend.repository.ApiTokenUserRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.specification.ApiTokenUserSpecification;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@ExtendWith(MockitoExtension.class)
class LongUserServiceTest {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
  private final long testId = 42L;

  @Mock BaseUserRepository<TestLongUser, Long> userRepositoryMock;
  @Mock ApiTokenUserRepository apiTokenUserRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock JwtTokenService jwtTokenServiceMock;
  @Mock RightService rightServiceMock;

  LongUserService SUT;

  @BeforeEach
  void setUp() {
    SUT =
        new LongUserService(
            userRepositoryMock,
            apiTokenUserRepositoryMock,
            passwordEncoderMock,
            userMailServiceMock,
            roleServiceMock,
            rightServiceMock,
            jwtTokenServiceMock);
  }

  private static final long TEST_USER_ID = 4711133742L;
  private static final String TEST_USERNAME = "devnull@frachtwerk.de";
  private static final String TEST_FIRST_NAME = "TEST_FIRST_NAME";
  private static final String TEST_LAST_NAME = "TEST_LAST_NAME";
  private static final String TEST_PHONE = "TEST_PHONE";
  private static final String TEST_MOBILE = "TEST_MOBILE";
  private static final String TEST_PASSWORD_PLAIN = "frachtwerk";
  private static final String TEST_PASSWORD_HASH =
      "{bcrypt}$2b$10$dwJpN2XigdXZLvviA4dIkOuQC31/8JdgD60o5uCYGT.OBn1WDtL9i";
  private static final String TEST_NONCE = "78fd553y";
  private final Locale TEST_LOCALE = Locale.CANADA_FRENCH;

  @Test
  @SuppressWarnings("unchecked")
  void getAll() {
    var pageableMock = Mockito.mock(Pageable.class);
    var pageMock = Mockito.mock(Page.class);
    // noinspection unchecked
    Mockito.when(pageMock.map(Mockito.any())).thenReturn(pageMock);

    Mockito.when(userRepositoryMock.findAll(pageableMock)).thenReturn(pageMock);

    Assertions.assertThat(SUT.getAll(pageableMock)).isEqualTo(pageMock);
  }

  @Nested
  class GetUserById {

    @Test
    void userPresent() {
      var mockUserResponse = mock(TestLongUser.class);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUserResponse));

      Assertions.assertThat(SUT.getById(testId)).isSameAs(mockUserResponse);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> SUT.getById(testId)).isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  class CreateUser {

    @Test
    void defaultUser() {
      final Role testRole = Role.builder().name("ROLE").description("ROLE").build();
      final String testUsername = "Elon.Musk@frachtwerk.de";
      final String testSource = "Straight outta Compton";
      var testSavedUser = mock(TestLongUser.class);

      when(roleServiceMock.getDefaultRole()).thenReturn(testRole);
      when(roleServiceMock.getByName(anyString()))
          .thenAnswer(
              invocationOnMock -> {
                final String name = invocationOnMock.getArgument(0);
                return Role.builder().name(name).description(name).build();
              });
      when(userRepositoryMock.save(
              MockitoHamcrest.argThat(
                  Matchers.allOf(
                      isA(TestLongUser.class),
                      hasProperty("email", Matchers.is(testUsername.toLowerCase())),
                      hasProperty("source", Matchers.is(testSource)),
                      hasProperty("roles", Matchers.contains(testRole))))))
          .thenReturn(testSavedUser);

      TestLongUser mockResult =
          SUT.createDefaultUser(
              UserInfoEssentials.builder().username(testUsername).build(), testSource);

      assertThat(mockResult).isEqualTo(testSavedUser);
      verify(userRepositoryMock, times(1)).save(any(TestLongUser.class));
    }

    @Test
    void customRole() {
      final Role testRole = Role.builder().name("SPECIAL_ROLE").build();
      final UserDto testUser = new UserDto<>();
      testUser.setEmail("test.user@frachtwerk.de");
      testUser.getRoles().add(testRole.getName());

      final var testSavedUser = mock(TestLongUser.class);

      when(roleServiceMock.getByName(any())).thenReturn(testRole);
      when(userRepositoryMock.save(
              MockitoHamcrest.argThat(
                  Matchers.allOf(
                      isA(TestLongUser.class),
                      hasProperty("email", Matchers.is(testUser.getEmail())),
                      hasProperty("roles", Matchers.contains(testRole)))))) // NOT the default role!
          .thenReturn(testSavedUser);

      final var mockResult = SUT.create(testUser);

      assertThat(mockResult).isEqualTo(testSavedUser);
      verify(userRepositoryMock, times(1)).save(any(TestLongUser.class));
    }

    @Test
    void passwordPresent() {
      UserDto testUser = new UserDto<>();

      String testPassword = "testPassword";
      String testEncodedPassword = "BANANARAMA";

      when(roleServiceMock.getDefaultRole()).thenReturn(new Role());
      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);

      testUser.setPassword(testPassword);
      final var testSavedUser = TestLongUser.builder().password(testUser.getPassword()).build();

      when(userRepositoryMock.save(any(TestLongUser.class)))
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                return testSavedUser;
              });

      SUT.create(testUser);
      verify(userRepositoryMock, times(1)).save(any(TestLongUser.class));
    }

    @Test
    void passwordNull() throws CheckedMailException {
      final UserDto<Long> testUser = new UserDto<>();

      final String testMail = "test_user@example.com";
      final String testPassword = null;
      final String testEncodedPassword = "BANANARAMA";

      when(roleServiceMock.getDefaultRole()).thenReturn(new Role());

      final AtomicReference<String> capturedPassword = new AtomicReference<>();
      when(passwordEncoderMock.encode(any()))
          .thenAnswer(
              invocationOnMock -> {
                capturedPassword.set(invocationOnMock.getArgument(0));

                return testEncodedPassword;
              });

      testUser.setEmail(testMail);
      testUser.setPassword(testPassword);

      final TestLongUser testSavedUser =
          TestLongUser.builder()
              .email(testUser.getEmail())
              .passwordResetToken(testUser.getPasswordResetToken())
              .password(testUser.getPassword())
              .build();

      final AtomicReference<TestLongUser> userToSave = new AtomicReference<>();
      when(userRepositoryMock.save(any(TestLongUser.class)))
          .thenAnswer(
              invocation -> {
                userToSave.set(invocation.getArgument(0));
                final TestLongUser toSave = userToSave.get();
                assertThat(toSave.getPasswordResetToken()).isNotNull();
                assertThat(toSave.getPasswordResetToken()).isNotBlank();
                assertThat(toSave.getPasswordResetToken()).matches(UUID_REGEX);
                assertThat(capturedPassword.get()).isNotNull();
                assertThat(capturedPassword.get()).isNotBlank();
                assertThat(capturedPassword.get().getBytes()).hasSizeGreaterThan(32);
                assertThat(capturedPassword.get()).isNotEqualTo(toSave.getPasswordResetToken());
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                testSavedUser.setPasswordResetToken(toSave.getPasswordResetToken());

                return testSavedUser;
              });

      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                assertThat(mail).isEqualTo(testMail);
                assertThat(token).isEqualTo(userToSave.get().getPasswordResetToken());
                return "";
              })
          .when(userMailServiceMock)
          .sendNewUserMail(anyString(), anyString(), any());

      Assertions.assertThat(SUT.create(testUser)).isSameAs(testSavedUser);
      verify(userMailServiceMock, times(1)).sendNewUserMail(anyString(), anyString(), any());
    }

    @Test
    void passwordEmpty() throws CheckedMailException {
      final UserDto testUser = new UserDto<>();

      final String testMail = "test_user@example.com";
      final String testPassword = null;
      final String testEncodedPassword = "BANANARAMA";

      when(roleServiceMock.getDefaultRole()).thenReturn(new Role());

      final AtomicReference<String> capturedPassword = new AtomicReference<>();
      when(passwordEncoderMock.encode(any()))
          .thenAnswer(
              invocationOnMock -> {
                capturedPassword.set(invocationOnMock.getArgument(0));

                return testEncodedPassword;
              });

      testUser.setEmail(testMail);
      testUser.setPassword(testPassword);

      final TestLongUser testSavedUser =
          TestLongUser.builder()
              .email(testUser.getEmail())
              .passwordResetToken(testUser.getPasswordResetToken())
              .password(testUser.getPassword())
              .build();

      final AtomicReference<TestLongUser> userToSave = new AtomicReference<>();
      when(userRepositoryMock.save(any(TestLongUser.class)))
          .thenAnswer(
              invocation -> {
                userToSave.set(invocation.getArgument(0));
                final var toSave = userToSave.get();
                assertThat(toSave.getPasswordResetToken()).isNotNull();
                assertThat(toSave.getPasswordResetToken()).isNotBlank();
                assertThat(toSave.getPasswordResetToken()).matches(UUID_REGEX);
                assertThat(capturedPassword.get()).isNotNull();
                assertThat(capturedPassword.get()).isNotBlank();
                assertThat(capturedPassword.get().getBytes()).hasSizeGreaterThan(32);
                assertThat(capturedPassword.get()).isNotEqualTo(toSave.getPasswordResetToken());
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                testSavedUser.setPasswordResetToken(toSave.getPasswordResetToken());

                return testSavedUser;
              });

      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                assertThat(mail).isEqualTo(testMail);
                assertThat(token).isEqualTo(userToSave.get().getPasswordResetToken());
                return "";
              })
          .when(userMailServiceMock)
          .sendNewUserMail(anyString(), anyString(), any(Locale.class));

      Assertions.assertThat(SUT.create(testUser)).isSameAs(testSavedUser);
      verify(userMailServiceMock, times(1))
          .sendNewUserMail(anyString(), anyString(), any(Locale.class));
    }

    @Test
    void externalAuth() {
      final UserDto testUser = new UserDto<>();
      testUser.setEmail("joe.biden@whitehouse.com");
      testUser.setSource("ldap");

      when(roleServiceMock.getDefaultRole()).thenReturn(new Role());
      when(userRepositoryMock.save(
              MockitoHamcrest.argThat(
                  Matchers.allOf(
                      isA(TestLongUser.class),
                      hasProperty("email", Matchers.is(testUser.getEmail())),
                      hasProperty("source", Matchers.is(testUser.getSource())),
                      hasProperty("password", Matchers.nullValue()),
                      hasProperty("passwordResetToken", Matchers.nullValue()),
                      hasProperty("roles", Matchers.notNullValue())))))
          .thenReturn(mock(TestLongUser.class));

      SUT.create(testUser);

      verify(userRepositoryMock, times(1)).save(any(TestLongUser.class));
      verifyNoInteractions(userMailServiceMock);
    }
  }

  @Nested
  class UpdateUser {
    private final UserDto userToUpdate = mock(UserDto.class);

    @Test
    void inconsistentId() {
      when(userToUpdate.getId()).thenReturn(testId + 42);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mock(TestLongUser.class)));

      assertThatThrownBy(() -> SUT.update(testId, userToUpdate))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void userNotFound() {
      when(userToUpdate.getId()).thenReturn(testId);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> SUT.update(testId, userToUpdate))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSuccessful() {
      when(userToUpdate.getId()).thenReturn(testId);

      final TestLongUser mockUser = mock(TestLongUser.class);
      when(mockUser.hasLocalAuthentication()).thenReturn(true);
      when(mockUser.getSource()).thenReturn(AbstractBaseUser.USER_AUTH_SOURCE_LOCAL);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUser));
      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));

      String testPassword = "testPassword";
      String testEncodedPassword = "BANANARAMA";

      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);

      when(userToUpdate.getPassword()).thenReturn(testPassword);
      when(userRepositoryMock.save(any(TestLongUser.class)))
          .thenAnswer(
              invocation -> {
                AbstractBaseUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                return toSave;
              });
      assertDoesNotThrow(() -> SUT.update(testId, userToUpdate));
    }

    @Test
    void testNoPasswordUpdateForExternalUser() {
      // we should not be able to update the password of a user sourced from oauth or ldap, as it
      // wouldn't make sense

      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));

      final String NEW_FIRST_NAME = "Tobi";

      final TestLongUser existingUser = mock(TestLongUser.class);
      when(existingUser.getNonce()).thenReturn(TEST_NONCE);
      when(existingUser.getSource()).thenReturn("ldap");

      final UserDto userUpdate =
          UserDto.builder()
              .id(TEST_USER_ID)
              .firstName(NEW_FIRST_NAME)
              .password("shouldbeignored")
              .build();

      when(userRepositoryMock.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));
      when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(i -> i.getArgument(0));

      final TestLongUser savedUser = SUT.update(TEST_USER_ID, userUpdate);
      assertEquals(TEST_USER_ID, savedUser.getId());
      assertEquals(NEW_FIRST_NAME, savedUser.getFirstName());
      assertEquals(TEST_NONCE, savedUser.getNonce());
      assertNull(savedUser.getPassword());

      verify(userRepositoryMock, times(3)).findById(anyLong());
      verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testNoPasswordPatchForExternalUser() {
      final String NEW_FIRST_NAME = "Tobi";

      final TestLongUser existingUser =
          TestLongUser.builder()
              .id(TEST_USER_ID)
              .email(TEST_USERNAME)
              .enabled(true)
              .firstName(TEST_FIRST_NAME)
              .lastName(TEST_LAST_NAME)
              .locale(TEST_LOCALE)
              .password(null)
              .passwordResetToken(null)
              .nonce(TEST_NONCE)
              .source("ldap")
              .build();

      final Map<String, Object> userUpdate =
          Map.of(
              "id", TEST_USER_ID,
              "firstName", NEW_FIRST_NAME,
              "password", "shouldbeignored");

      when(userRepositoryMock.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));
      when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(i -> i.getArgument(0));

      final TestLongUser savedUser = SUT.patch(TEST_USER_ID, userUpdate);
      assertEquals(TEST_USER_ID, savedUser.getId());
      assertEquals(TEST_NONCE, savedUser.getNonce());
      assertNull(savedUser.getPassword());

      verify(userRepositoryMock, times(2)).findById(anyLong());
      verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Nested
  class UpdateUserFields {

    private final TestLongUser testUser = TestLongUser.builder().email("Don´t care!").build();

    private Map<String, Object> testMap;

    @BeforeEach
    void setUp() {
      testMap = new LinkedHashMap<>();
    }

    @Test
    void userNotFound() {

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> SUT.patch(testId, testMap))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownField() {
      testMap.put("UNKNOWN_FIELD", "Don´t care");

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> SUT.patch(testId, testMap))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void updateSuccessful() {
      String testFirstName = "Peter";
      String testLastName = "Zwegat";
      String testPhone = "555-1337424711";
      String testPassword = "testPassword";

      testMap.put("firstName", testFirstName);
      testMap.put("lastName", testLastName);
      testMap.put("phone", testPhone);
      testMap.put("password", testPassword);

      String testEncodedPassword = "BANANARAMA";

      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));

      testUser.setPassword(testPassword);
      when(userRepositoryMock.save(testUser))
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);
                assertThat(toSave.getFirstName()).isEqualTo(testFirstName);
                assertThat(toSave.getLastName()).isEqualTo(testLastName);
                assertThat(toSave.getPhone()).isEqualTo(testPhone);

                return toSave;
              });
      assertDoesNotThrow(() -> SUT.patch(testId, testMap));
    }
  }

  @Nested
  class GetCurrentLoggedInUser {
    @Test
    void noUserLoggedIn() {
      assertThatThrownBy(() -> SUT.getUserFromPrincipal(null))
          .isInstanceOf(SessionAuthenticationException.class);
    }

    @Test
    void userWronglyLoggedIn() {
      var testPrincipal = mock(UsernamePasswordAuthenticationToken.class);

      when(testPrincipal.getPrincipal()).thenReturn(null);

      assertThatThrownBy(() -> SUT.getUserFromPrincipal(testPrincipal))
          .isInstanceOf(SessionAuthenticationException.class);
    }

    @Test
    void userIsLoggedIn() {
      var testPrincipal = mock(UsernamePasswordAuthenticationToken.class);
      var testUser = mock(TestLongUser.class);

      when(testPrincipal.getPrincipal()).thenReturn(testUser);

      Assertions.assertThat(SUT.getUserFromPrincipal(testPrincipal)).isSameAs(testUser);
    }
  }

  @Nested
  class LoadUserByUsername {

    private final String testUsername = "test@example.com";

    @Test
    void userPresent() {
      var mockUserResponse = mock(TestLongUser.class);

      when(userRepositoryMock.findByEmailIgnoreCase(testUsername))
          .thenReturn(Optional.of(mockUserResponse));

      Assertions.assertThat(SUT.loadUserByUsername(testUsername)).isSameAs(mockUserResponse);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findByEmailIgnoreCase(testUsername)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> SUT.loadUserByUsername(testUsername))
          .isInstanceOf(UsernameNotFoundException.class);
    }
  }

  @Test
  void deleteUserById() {
    when(userRepositoryMock.existsById(testId)).thenReturn(true);
    doNothing().when(userRepositoryMock).deleteById(testId);

    SUT.deleteById(testId);

    verify(userRepositoryMock).deleteById(testId);
  }

  @Nested
  class CreateResetPasswordToken {

    private final String testUsername = "test@example.com";

    @Test
    void successful() throws CheckedMailException {
      TestLongUser testUser = mock(TestLongUser.class);
      var locale = Locale.GERMANY;
      when(userRepositoryMock.findByEmailIgnoreCase(testUsername))
          .thenReturn(Optional.of(testUser));
      when(testUser.hasLocalAuthentication()).thenReturn(true);
      when(testUser.getLocale()).thenReturn(locale);
      doAnswer(
              invocationOnMock -> {
                var token = invocationOnMock.getArgument(0, String.class);
                assertThat(token).matches(UUID_REGEX);
                return "";
              })
          .when(testUser)
          .setPasswordResetToken(any());

      var savedUser = mock(TestLongUser.class);
      when(userRepositoryMock.save(testUser)).thenReturn(savedUser);

      var savedToken = "BANANARAMA";
      when(savedUser.getPasswordResetToken()).thenReturn(savedToken);

      doAnswer(
              invocationOnMock -> {
                var mail = invocationOnMock.getArgument(0, String.class);
                var token = invocationOnMock.getArgument(1, String.class);

                assertThat(token).isEqualTo(savedToken);
                assertThat(mail).isEqualTo(testUsername);

                return "";
              })
          .when(userMailServiceMock)
          .sendResetToken(testUsername, savedToken, locale);

      SUT.createResetPasswordToken(testUsername);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findByEmailIgnoreCase(testUsername)).thenReturn(Optional.empty());
      assertThrows(
          UsernameNotFoundException.class, () -> SUT.createResetPasswordToken(testUsername));
      verify(userRepositoryMock, times(1)).findByEmailIgnoreCase(anyString());
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Nested
  class SelfUpdate {
    private final TestLongUser testUser = TestLongUser.builder().email(TEST_USERNAME).build();
    private final UsernamePasswordAuthenticationToken testPrincipal =
        mock(UsernamePasswordAuthenticationToken.class);

    @BeforeEach
    void setUp() {
      testUser.setId(TEST_USER_ID);
      testUser.setFirstName(TEST_FIRST_NAME);
      testUser.setLastName(TEST_LAST_NAME);
      testUser.setPhone(TEST_PHONE);
      testUser.setMobile(TEST_MOBILE);
      testUser.setLocale(TEST_LOCALE);
      testUser.setPassword(TEST_PASSWORD_HASH);

      when(testPrincipal.getPrincipal()).thenReturn(testUser);
    }

    @Test
    void testExceptionOnNullUser() {
      assertThatThrownBy(() -> SUT.selfUpdate(null, new UserDto<>()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUpdateUserByDto() {
      final String NEW_FIRST_NAME = "Robin";
      final String NEW_LAST_NAME = "The Ripper";
      final String NEW_PHONE = "018012345";
      final String NEW_MOBILE = "018098765";
      final Locale NEW_LOCALE = Locale.ITALY;
      final String NEW_PASSWORD = "hopefully not working!";

      final UserDto updates = new UserDto<>();
      updates.setFirstName(NEW_FIRST_NAME);
      updates.setLastName(NEW_LAST_NAME);
      updates.setPhone(NEW_PHONE);
      updates.setMobile(NEW_MOBILE);
      updates.setLocale(NEW_LOCALE);
      updates.setPassword(NEW_PASSWORD);

      when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(c -> c.getArgument(0));

      final TestLongUser result =
          SUT.selfUpdate((TestLongUser) testPrincipal.getPrincipal(), updates);
      assertThat(result.getFirstName()).isEqualTo(NEW_FIRST_NAME);
      assertThat(result.getLastName()).isEqualTo(NEW_LAST_NAME);
      assertThat(result.getPhone()).isEqualTo(NEW_PHONE);
      assertThat(result.getMobile()).isEqualTo(NEW_MOBILE);
      assertThat(result.getLocale()).isEqualTo(NEW_LOCALE);
      assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD_HASH);

      verify(userRepositoryMock).save(any(TestLongUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdateUserByFields() {
      final String NEW_FIRST_NAME = "Robin";
      final String NEW_LAST_NAME = "The Ripper";
      final String NEW_PHONE = "018012345";
      final String NEW_MOBILE = "018098765";
      final Locale NEW_LOCALE = Locale.ITALY;
      final String NEW_PASSWORD = "hopefully not working!";

      final Map<String, Object> updates =
          Map.of(
              "firstName", NEW_FIRST_NAME,
              "lastName", NEW_LAST_NAME,
              "phone", NEW_PHONE,
              "mobile", NEW_MOBILE,
              "locale", NEW_LOCALE,
              "password", NEW_PASSWORD);

      when(userRepositoryMock.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
      when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(c -> c.getArgument(0));

      final TestLongUser result =
          SUT.selfUpdate((TestLongUser) testPrincipal.getPrincipal(), updates);
      assertThat(result.getFirstName()).isEqualTo(NEW_FIRST_NAME);
      assertThat(result.getLastName()).isEqualTo(NEW_LAST_NAME);
      assertThat(result.getPhone()).isEqualTo(NEW_PHONE);
      assertThat(result.getMobile()).isEqualTo(NEW_MOBILE);
      assertThat(result.getLocale()).isEqualTo(NEW_LOCALE);
      assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD_HASH);

      verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
      verify(userRepositoryMock, times(2)).findById(anyLong());
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordWrongCredentials() {
      final String NEW_PASSWORD = "secret password!";

      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest(NEW_PASSWORD, "wrong password");
      assertThrows(
          BadCredentialsException.class,
          () -> SUT.updatePassword((TestLongUser) testPrincipal.getPrincipal(), updateRequest));

      verifyNoInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordSuccess() {
      final String NEW_PASSWORD_PLAIN = "secret password!";
      final String NEW_PASSWORD_HASH =
          "$2b$10$9yPzo9U15R1jqm6Db6Jg4uZTlvedRNii/orl4bQfY.nUBM5hL0/9a";

      when(passwordEncoderMock.matches(TEST_PASSWORD_PLAIN, TEST_PASSWORD_HASH)).thenReturn(true);
      when(passwordEncoderMock.encode(NEW_PASSWORD_PLAIN)).thenReturn(NEW_PASSWORD_HASH);
      when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(c -> c.getArgument(0));
      when(userRepositoryMock.findById(anyLong())).thenReturn(Optional.of(testUser));

      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest(NEW_PASSWORD_PLAIN, TEST_PASSWORD_PLAIN);
      final TestLongUser result =
          SUT.updatePassword((TestLongUser) testPrincipal.getPrincipal(), updateRequest);

      assertThat(result.getPassword()).isEqualTo(NEW_PASSWORD_HASH);
      assertThat(result.getNonce()).isNotEmpty();
      assertThat(result.getNonce()).isNotEqualTo(TEST_NONCE);

      verify(passwordEncoderMock).matches(anyString(), anyString());
      verify(passwordEncoderMock).encode(anyString());
      verify(userRepositoryMock).save(any(TestLongUser.class));
      verify(userRepositoryMock).findById(anyLong());
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordFailForExternalUser() {
      testUser.setSource("ldap");

      final PasswordUpdateRequest updateRequest =
          new PasswordUpdateRequest("shouldbeignored", TEST_PASSWORD_PLAIN);

      assertThrows(
          NotAllowedException.class,
          () -> SUT.updatePassword((TestLongUser) testPrincipal.getPrincipal(), updateRequest));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Test
  void getTokensTest() {
    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());

    when(jwtTokenServiceMock.getTokens(user.getUsername(), SessionTokenType.REFRESH))
        .thenReturn(List.of(SessionToken.builder().build()));

    List<SessionToken> tokens = SUT.getTokens(user, SessionTokenType.REFRESH);

    assertThat(tokens).isNotEmpty().hasSize(1);
    verify(jwtTokenServiceMock, times(1)).getTokens(user.getUsername(), SessionTokenType.REFRESH);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoInteractions(userRepositoryMock);
  }

  @Test
  void deleteToken() {
    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    UUID uuid = UUID.randomUUID();
    SUT.deleteToken(user, uuid);
    verify(jwtTokenServiceMock, times(1)).deleteToken(user.getUsername(), uuid);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoInteractions(userRepositoryMock);
  }

  @Test
  void createApiTokenSuccess() {
    ApiTokenUserDto apiTokenUserDto =
        ApiTokenUserDto.builder()
            .id(null)
            .description("description")
            .rights(
                new HashSet<>(
                    Set.of(
                        BasicApplicationRight.TRANSLATION_CREATE.name(),
                        BasicApplicationRight.TRANSLATION_UPDATE.name(),
                        BasicApplicationRight.TRANSLATION_DELETE.name(),
                        BasicApplicationRight.TRANSLATION_READ.name())))
            .validUntil(LocalDate.now().plusWeeks(1))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setEmail("user@app.com");
    user.setRoles(
        Set.of(
            Role.builder()
                .rights(
                    new HashSet<>(
                        List.of(
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                                .description("TRANSLATION_CREATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                                .description("TRANSLATION_UPDATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                                .description("TRANSLATION_DELETE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_READ.name())
                                .description("TRANSLATION_READ")
                                .build())))
                .build()));

    when(apiTokenUserRepositoryMock.existsByLinkedUserAndDescription(anyString(), anyString()))
        .thenReturn(false);
    when(rightServiceMock.findByAuthority(anyString()))
        .thenAnswer(
            invocation -> {
              String authority = invocation.getArgument(0);
              return Right.builder().authority(authority).description(authority).build();
            });
    when(apiTokenUserRepositoryMock.save(any(ApiTokenUser.class)))
        .thenAnswer(
            invocation -> {
              ApiTokenUser apiTokenUser = invocation.getArgument(0);
              apiTokenUser.setId(UUID.randomUUID());
              return apiTokenUser;
            });
    when(jwtTokenServiceMock.createToken(
            any(ApiTokenUser.class),
            any(SessionTokenType.class),
            any(),
            any(),
            any(LocalDate.class)))
        .thenReturn("token");

    ApiTokenUserRepresentation apiTokenUserRepresentation =
        SUT.createApiToken(user, apiTokenUserDto);

    assertNotNull(apiTokenUserRepresentation);
    assertNotNull(apiTokenUserRepresentation.getId());
    assertEquals(apiTokenUserDto.getDescription(), apiTokenUserRepresentation.getDescription());
    assertEquals(apiTokenUserDto.getRights().size(), apiTokenUserRepresentation.getRights().size());
    assertEquals(apiTokenUserDto.getValidUntil(), apiTokenUserRepresentation.getValidUntil());
    assertFalse(apiTokenUserRepresentation.isDisabled());

    verify(apiTokenUserRepositoryMock, times(1))
        .existsByLinkedUserAndDescription(anyString(), anyString());
    verify(rightServiceMock, times(4)).findByAuthority(anyString());
    verify(apiTokenUserRepositoryMock, times(1)).save(any(ApiTokenUser.class));
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);
    verifyNoMoreInteractions(rightServiceMock);
  }

  @Test
  void createApiTokenEmptyRights() {
    ApiTokenUserDto apiTokenUserDto =
        ApiTokenUserDto.builder()
            .id(null)
            .description("description")
            .rights(new HashSet<>())
            .validUntil(LocalDate.now().plusWeeks(1))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setEmail("user@app.com");
    user.setRoles(
        Set.of(
            Role.builder()
                .rights(
                    new HashSet<>(
                        List.of(
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                                .description("TRANSLATION_CREATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                                .description("TRANSLATION_UPDATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                                .description("TRANSLATION_DELETE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_READ.name())
                                .description("TRANSLATION_READ")
                                .build())))
                .build()));

    when(apiTokenUserRepositoryMock.existsByLinkedUserAndDescription(anyString(), anyString()))
        .thenReturn(false);

    String message =
        assertThrows(InvalidInputException.class, () -> SUT.createApiToken(user, apiTokenUserDto))
            .getMessage();
    assertEquals("At least one right must be selected", message);
  }

  @Test
  void createApiTokenInvalidRights() {
    ApiTokenUserDto apiTokenUserDto =
        ApiTokenUserDto.builder()
            .id(null)
            .description("description")
            .rights(
                new HashSet<>(
                    Set.of(
                        BasicApplicationRight.USER_CREATE.name(),
                        BasicApplicationRight.USER_API_TOKEN_CREATE.name())))
            .validUntil(LocalDate.now().plusWeeks(1))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setEmail("user@app.com");
    user.setRoles(
        Set.of(
            Role.builder()
                .rights(
                    new HashSet<>(
                        List.of(
                            Right.builder()
                                .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                                .description("USER_TOKEN_CREATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                                .description("TRANSLATION_CREATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                                .description("TRANSLATION_UPDATE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                                .description("TRANSLATION_DELETE")
                                .build(),
                            Right.builder()
                                .authority(BasicApplicationRight.TRANSLATION_READ.name())
                                .description("TRANSLATION_READ")
                                .build())))
                .build()));

    when(apiTokenUserRepositoryMock.existsByLinkedUserAndDescription(anyString(), anyString()))
        .thenReturn(false);
    when(rightServiceMock.findByAuthority(anyString()))
        .thenAnswer(
            invocation -> {
              String authority = invocation.getArgument(0);
              return Right.builder().authority(authority).description(authority).build();
            });

    String message =
        assertThrows(InvalidInputException.class, () -> SUT.createApiToken(user, apiTokenUserDto))
            .getMessage();
    assertEquals("At least one right must be selected", message);
  }

  @Test
  void createApiTokenDuplicateConstraint() {
    ApiTokenUserDto apiTokenUserDto =
        ApiTokenUserDto.builder()
            .id(null)
            .description("description")
            .rights(Set.of())
            .validUntil(LocalDate.now().plusWeeks(1))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setEmail("user@app.com");

    when(apiTokenUserRepositoryMock.existsByLinkedUserAndDescription(anyString(), anyString()))
        .thenReturn(true);

    String message =
        assertThrows(InvalidInputException.class, () -> SUT.createApiToken(user, apiTokenUserDto))
            .getMessage();
    assertEquals("A token with this description already exists", message);
  }

  @Test
  void getApiTokens() {
    ApiTokenUserSpecification specification = mock(ApiTokenUserSpecification.class);
    Pageable pageable = PageRequest.of(0, 10);
    ApiTokenUser apiTokenUser =
        ApiTokenUser.builder()
            .id(UUID.randomUUID())
            .description("description")
            .rights(Set.of())
            .validUntil(LocalDate.now().plusWeeks(1))
            .build();
    Page<ApiTokenUser> page = new PageImpl<>(List.of(apiTokenUser), pageable, 1);
    when(apiTokenUserRepositoryMock.findAll(specification, pageable)).thenReturn(page);

    Page<ApiTokenUserRepresentation> result = SUT.getApiTokens(specification, pageable);

    verify(apiTokenUserRepositoryMock, times(1)).findAll(specification, pageable);
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isNotEmpty();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0)).isNotNull();
    assertThat(result.getContent().get(0).getDescription())
        .isEqualTo(apiTokenUser.getDescription());
  }

  @Test
  void deleteApiToken() {
    UUID id = UUID.randomUUID();
    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setId(1L);
    user.setEmail("user@app.com");

    when(apiTokenUserRepositoryMock.findById(id))
        .thenReturn(
            Optional.of(ApiTokenUser.builder().id(id).linkedUser(user.getUsername()).build()));

    SUT.deleteApiToken(user, id);

    verify(apiTokenUserRepositoryMock, times(1)).findById(id);
    verify(apiTokenUserRepositoryMock, times(1)).delete(any(ApiTokenUser.class));
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);
  }

  @Test
  void deleteApiTokenNotFound() {
    UUID id = UUID.randomUUID();
    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setId(1L);
    user.setEmail("user@app.com");

    when(apiTokenUserRepositoryMock.findById(id)).thenReturn(Optional.empty());

    String message =
        assertThrows(ResourceNotFoundException.class, () -> SUT.deleteApiToken(user, id))
            .getMessage();

    assertEquals("ApiTokenUser not found", message);

    verify(apiTokenUserRepositoryMock, times(1)).findById(id);
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);
  }

  @Test
  void deleteApiTokenWrongUser() {
    UUID id = UUID.randomUUID();
    TestLongUser user1 = SUT.convertDtoToEntity(SUT.getNewUser());
    user1.setId(1L);
    user1.setEmail("user1@app.com");

    TestLongUser user2 = SUT.convertDtoToEntity(SUT.getNewUser());
    user2.setId(1L);
    user2.setEmail("user2@app.com");

    when(apiTokenUserRepositoryMock.findById(id))
        .thenReturn(
            Optional.of(ApiTokenUser.builder().id(id).linkedUser(user1.getUsername()).build()));

    String message =
        assertThrows(NotAllowedException.class, () -> SUT.deleteApiToken(user2, id)).getMessage();

    assertEquals("You are not allowed to disable this token", message);

    verify(apiTokenUserRepositoryMock, times(1)).findById(id);
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);
  }

  @Test
  void updateApiTokens() {
    Role oldRole =
        Role.builder()
            .name("OLD_ROLE")
            .rights(
                new HashSet<>(
                    List.of(
                        Right.builder()
                            .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                            .description("TRANSLATION_CREATE")
                            .build(),
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                            .description("TRANSLATION_CREATE")
                            .build(),
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                            .description("TRANSLATION_UPDATE")
                            .build(),
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                            .description("TRANSLATION_DELETE")
                            .build(),
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_READ.name())
                            .description("TRANSLATION_READ")
                            .build())))
            .build();
    Role newRole =
        Role.builder()
            .name("NEW_ROLE")
            .rights(
                new HashSet<>(
                    List.of(
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_READ.name())
                            .description("TRANSLATION_READ")
                            .build())))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setId(1L);
    user.setEmail("user@app.com");
    user.setRoles(Set.of(oldRole));

    UserDto userToUpdate = mock(UserDto.class);

    when(userToUpdate.getId()).thenReturn(1L);
    when(userToUpdate.getRoles()).thenReturn(Set.of("NEW_ROLE"));
    when(userToUpdate.getEmail()).thenReturn(user.getEmail());

    when(userRepositoryMock.findById(1L)).thenReturn(Optional.of(user));
    when(roleServiceMock.getByName("NEW_ROLE")).thenReturn(newRole);
    when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(i -> i.getArgument(0));

    when(apiTokenUserRepositoryMock.findByLinkedUser(user.getEmail()))
        .thenReturn(
            List.of(
                ApiTokenUser.builder()
                    .id(UUID.randomUUID())
                    .description("large token")
                    .linkedUser(user.getEmail())
                    .rights(
                        new HashSet<>(
                            List.of(
                                Right.builder()
                                    .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                                    .description("TRANSLATION_CREATE")
                                    .build(),
                                Right.builder()
                                    .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                                    .description("TRANSLATION_UPDATE")
                                    .build(),
                                Right.builder()
                                    .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                                    .description("TRANSLATION_DELETE")
                                    .build(),
                                Right.builder()
                                    .authority(BasicApplicationRight.TRANSLATION_READ.name())
                                    .description("TRANSLATION_READ")
                                    .build())))
                    .createdAt(LocalDateTime.now().minusWeeks(1))
                    .validUntil(LocalDate.now().plusWeeks(1))
                    .description("description")
                    .disabled(false)
                    .build(),
                ApiTokenUser.builder()
                    .id(UUID.randomUUID())
                    .description("small token")
                    .linkedUser(user.getEmail())
                    .rights(
                        new HashSet<>(
                            List.of(
                                Right.builder()
                                    .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                                    .description("USER_API_TOKEN_CREATE")
                                    .build())))
                    .createdAt(LocalDateTime.now().minusWeeks(1))
                    .validUntil(LocalDate.now().plusWeeks(1))
                    .description("description")
                    .disabled(false)
                    .build()));

    TestLongUser update = SUT.update(1L, userToUpdate);

    verify(userRepositoryMock, times(3)).findById(1L);
    verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
    verifyNoMoreInteractions(userRepositoryMock);
    verify(apiTokenUserRepositoryMock, times(1)).findByLinkedUser(user.getUsername());
    verify(apiTokenUserRepositoryMock, times(1)).delete(any(ApiTokenUser.class));
    verify(apiTokenUserRepositoryMock, times(1)).save(any(ApiTokenUser.class));
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);

    assertThat(update).isNotNull();
  }

  @Test
  void updateApiTokensPut() {
    Role role =
        Role.builder()
            .name("ROLE")
            .rights(
                new HashSet<>(
                    List.of(
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_READ.name())
                            .description("TRANSLATION_READ")
                            .build())))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setId(1L);
    user.setEmail("user_old@app.com");
    user.setRoles(Set.of(role));

    UserDto userToUpdate = mock(UserDto.class);

    when(userToUpdate.getId()).thenReturn(1L);
    when(userToUpdate.getRoles()).thenReturn(Set.of("ROLE"));
    when(userToUpdate.getEmail()).thenReturn("user_new@app.com");

    when(userRepositoryMock.findById(1L)).thenReturn(Optional.of(user));
    when(roleServiceMock.getByName("ROLE")).thenReturn(role);
    when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(i -> i.getArgument(0));

    when(apiTokenUserRepositoryMock.findByLinkedUser(user.getEmail()))
        .thenReturn(
            List.of(
                ApiTokenUser.builder()
                    .id(UUID.randomUUID())
                    .description("small token")
                    .linkedUser(user.getEmail())
                    .rights(
                        new HashSet<>(
                            List.of(
                                Right.builder()
                                    .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                                    .description("TRANSLATION_CREATE")
                                    .build())))
                    .createdAt(LocalDateTime.now().minusWeeks(1))
                    .validUntil(LocalDate.now().plusWeeks(1))
                    .description("description")
                    .disabled(false)
                    .build()));

    TestLongUser update = SUT.update(1L, userToUpdate);

    verify(userRepositoryMock, times(4)).findById(1L);
    verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
    verifyNoMoreInteractions(userRepositoryMock);
    verify(apiTokenUserRepositoryMock, times(1)).findByLinkedUser("user_old@app.com");
    verify(apiTokenUserRepositoryMock, times(1)).findByLinkedUser("user_new@app.com");
    verify(apiTokenUserRepositoryMock, times(1)).deleteAll(anyList());
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);

    assertThat(update).isNotNull();
  }

  @Test
  void deleteAllApiTokensPatch() {
    Role oldRole =
        Role.builder()
            .name("ROLE")
            .rights(
                new HashSet<>(
                    List.of(
                        Right.builder()
                            .authority(BasicApplicationRight.TRANSLATION_READ.name())
                            .description("TRANSLATION_READ")
                            .build())))
            .build();

    TestLongUser user = SUT.convertDtoToEntity(SUT.getNewUser());
    user.setId(1L);
    user.setEmail("user_old@app.com");
    user.setRoles(new HashSet<>(List.of(oldRole)));

    when(userRepositoryMock.findById(1L)).thenReturn(Optional.of(user));
    when(userRepositoryMock.save(any(TestLongUser.class))).thenAnswer(i -> i.getArgument(0));
    when(apiTokenUserRepositoryMock.findByLinkedUser(user.getEmail()))
        .thenReturn(
            List.of(
                ApiTokenUser.builder()
                    .id(UUID.randomUUID())
                    .description("token")
                    .linkedUser(user.getEmail())
                    .rights(
                        new HashSet<>(
                            List.of(
                                Right.builder()
                                    .authority(BasicApplicationRight.USER_API_TOKEN_CREATE.name())
                                    .description("TRANSLATION_CREATE")
                                    .build())))
                    .createdAt(LocalDateTime.now().minusWeeks(1))
                    .validUntil(LocalDate.now().plusWeeks(1))
                    .description("description")
                    .disabled(false)
                    .build()));

    TestLongUser update = SUT.patch(1L, Map.of("email", "user_new@app.com"));

    verify(userRepositoryMock, times(3)).findById(1L);
    verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
    verifyNoMoreInteractions(userRepositoryMock);
    verify(apiTokenUserRepositoryMock, times(1)).findByLinkedUser("user_old@app.com");
    verify(apiTokenUserRepositoryMock, times(1)).findByLinkedUser("user_new@app.com");
    verify(apiTokenUserRepositoryMock, times(1)).deleteAll(anyList());
    verifyNoMoreInteractions(apiTokenUserRepositoryMock);

    assertThat(update).isNotNull();
  }
}
