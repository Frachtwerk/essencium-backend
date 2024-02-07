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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@ExtendWith(MockitoExtension.class)
class UUIDUserServiceTest {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
  private final UUID testId = UUID.randomUUID();

  @Mock BaseUserRepository<TestUUIDUser, UUID> userRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock JwtTokenService jwtTokenServiceMock;

  UUIDUserService testSubject;

  @BeforeEach
  void setUp() {
    testSubject =
        new UUIDUserService(
            userRepositoryMock,
            passwordEncoderMock,
            userMailServiceMock,
            roleServiceMock,
            jwtTokenServiceMock);
  }

  private static final UUID TEST_USER_ID = UUID.randomUUID();
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

    Assertions.assertThat(testSubject.getAll(pageableMock)).isEqualTo(pageMock);
  }

  @Nested
  class GetUserById {

    @Test
    void userPresent() {
      var mockUserResponse = mock(TestUUIDUser.class);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUserResponse));

      Assertions.assertThat(testSubject.getById(testId)).isSameAs(mockUserResponse);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.getById(testId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  class CreateUser {

    @Test
    void defaultUser() {
      final Role testRole = Role.builder().name("ROLE").description("ROLE").build();
      final String testUsername = "Elon.Musk@frachtwerk.de";
      final String testSource = "Straight outta Compton";
      var testSavedUser = mock(TestUUIDUser.class);

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
                      isA(TestUUIDUser.class),
                      hasProperty("email", Matchers.is(testUsername.toLowerCase())),
                      hasProperty("source", Matchers.is(testSource)),
                      hasProperty("roles", Matchers.contains(testRole))))))
          .thenReturn(testSavedUser);

      TestUUIDUser mockResult =
          testSubject.createDefaultUser(
              UserInfoEssentials.builder().username(testUsername).build(), testSource);

      assertThat(mockResult).isEqualTo(testSavedUser);
      verify(userRepositoryMock, times(1)).save(any(TestUUIDUser.class));
    }

    @Test
    void customRole() {
      final Role testRole = Role.builder().name("SPECIAL_ROLE").build();
      final UserDto testUser = new UserDto<>();
      testUser.setEmail("test.user@frachtwerk.de");
      testUser.getRoles().add(testRole.getName());

      final var testSavedUser = mock(TestUUIDUser.class);

      when(roleServiceMock.getByName(any())).thenReturn(testRole);
      when(userRepositoryMock.save(
              MockitoHamcrest.argThat(
                  Matchers.allOf(
                      isA(TestUUIDUser.class),
                      hasProperty("email", Matchers.is(testUser.getEmail())),
                      hasProperty("roles", Matchers.contains(testRole))))))
          .thenReturn(testSavedUser);

      final var mockResult = testSubject.create(testUser);

      assertThat(mockResult).isEqualTo(testSavedUser);
      verify(userRepositoryMock, times(1)).save(any(TestUUIDUser.class));
    }

    @Test
    void passwordPresent() {
      UserDto testUser = new UserDto<>();

      var testPassword = "testPassword";
      var testEncodedPassword = "BANANARAMA";

      when(roleServiceMock.getDefaultRole()).thenReturn(new Role());
      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);

      testUser.setPassword(testPassword);
      final TestUUIDUser testSavedUser =
          TestUUIDUser.builder().password(testUser.getPassword()).build();

      when(userRepositoryMock.save(any(TestUUIDUser.class)))
          .thenAnswer(
              invocation -> {
                TestUUIDUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                return testSavedUser;
              });

      testSubject.create(testUser);
      verify(userRepositoryMock, times(1)).save(any(TestUUIDUser.class));
    }

    @Test
    void passwordNull() throws CheckedMailException {
      final UserDto testUser = new UserDto<>();

      final var testMail = "test_user@example.com";
      final String testPassword = null;
      final var testEncodedPassword = "BANANARAMA";

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

      final var testSavedUser =
          TestUUIDUser.builder()
              .email(testUser.getEmail())
              .passwordResetToken(testUser.getPasswordResetToken())
              .password(testUser.getPassword())
              .build();

      final AtomicReference<TestUUIDUser> userToSave = new AtomicReference<>();
      when(userRepositoryMock.save(any(TestUUIDUser.class)))
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
          .sendNewUserMail(anyString(), anyString(), any());

      Assertions.assertThat(testSubject.create(testUser)).isSameAs(testSavedUser);
      verify(userMailServiceMock, times(1)).sendNewUserMail(anyString(), anyString(), any());
    }

    @Test
    void passwordEmpty() throws CheckedMailException {
      final UserDto testUser = new UserDto<>();

      final var testMail = "test_user@example.com";
      final String testPassword = null;
      final var testEncodedPassword = "BANANARAMA";

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

      final var testSavedUser =
          TestUUIDUser.builder()
              .email(testUser.getEmail())
              .passwordResetToken(testUser.getPasswordResetToken())
              .password(testUser.getPassword())
              .build();

      final AtomicReference<TestUUIDUser> userToSave = new AtomicReference<>();
      when(userRepositoryMock.save(any(TestUUIDUser.class)))
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

      Assertions.assertThat(testSubject.create(testUser)).isSameAs(testSavedUser);
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
                      isA(TestUUIDUser.class),
                      hasProperty("email", Matchers.is(testUser.getEmail())),
                      hasProperty("source", Matchers.is(testUser.getSource())),
                      hasProperty("password", Matchers.nullValue()),
                      hasProperty("passwordResetToken", Matchers.nullValue()),
                      hasProperty("roles", Matchers.notNullValue())))))
          .thenReturn(mock(TestUUIDUser.class));

      testSubject.create(testUser);

      verify(userRepositoryMock, times(1)).save(any(TestUUIDUser.class));
      verifyNoInteractions(userMailServiceMock);
    }
  }

  @Nested
  class UpdateUser {
    private final UserDto userToUpdate = mock(UserDto.class);

    @Test
    void inconsistentId() {
      when(userToUpdate.getId()).thenReturn(UUID.randomUUID());

      assertThatThrownBy(() -> testSubject.update(testId, userToUpdate))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void userNotFound() {
      when(userToUpdate.getId()).thenReturn(testId);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.update(testId, userToUpdate))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSuccessful() {
      when(userToUpdate.getId()).thenReturn(testId);

      final var mockUser = mock(TestUUIDUser.class);
      when(mockUser.hasLocalAuthentication()).thenReturn(true);
      when(mockUser.getSource()).thenReturn(TestUUIDUser.USER_AUTH_SOURCE_LOCAL);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUser));
      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));

      var testPassword = "testPassword";
      var testEncodedPassword = "BANANARAMA";

      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);

      when(userToUpdate.getPassword()).thenReturn(testPassword);
      when(userRepositoryMock.save(any(TestUUIDUser.class)))
          .thenAnswer(
              invocation -> {
                AbstractBaseUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                return toSave;
              });
      assertDoesNotThrow(() -> testSubject.update(testId, userToUpdate));
    }

    @Test
    void testNoPasswordUpdateForExternalUser() {
      // we should not be able to update the password of a user sourced from oauth or ldap, as it
      // wouldn't make sense

      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));

      final var NEW_FIRST_NAME = "Tobi";

      final var existingUser = mock(TestUUIDUser.class);
      when(existingUser.getSource()).thenReturn("ldap");
      when(existingUser.getNonce()).thenReturn(TEST_NONCE);

      final UserDto userUpdate =
          UserDto.builder()
              .id(TEST_USER_ID)
              .firstName(NEW_FIRST_NAME)
              .password("shouldbeignored")
              .build();

      when(userRepositoryMock.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));
      when(userRepositoryMock.save(any(TestUUIDUser.class))).thenAnswer(i -> i.getArgument(0));

      final var savedUser = testSubject.update(TEST_USER_ID, userUpdate);
      assertEquals(TEST_USER_ID, savedUser.getId());
      assertEquals(NEW_FIRST_NAME, savedUser.getFirstName());
      assertEquals(TEST_NONCE, savedUser.getNonce());
      assertNull(savedUser.getPassword());

      verify(userRepositoryMock, times(3)).findById(any());
      verify(userRepositoryMock, times(2)).save(any(TestUUIDUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testNoPasswordPatchForExternalUser() {
      final var NEW_FIRST_NAME = "Tobi";

      final var existingUser =
          TestUUIDUser.builder()
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
      when(userRepositoryMock.save(any(TestUUIDUser.class))).thenAnswer(i -> i.getArgument(0));

      final var savedUser = testSubject.patch(TEST_USER_ID, userUpdate);
      assertEquals(TEST_USER_ID, savedUser.getId());
      assertEquals(TEST_NONCE, savedUser.getNonce());
      assertNull(savedUser.getPassword());

      verify(userRepositoryMock, times(2)).findById(any());
      verify(userRepositoryMock, times(2)).save(any(TestUUIDUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Nested
  class UpdateUserFields {

    private final TestUUIDUser testUser = TestUUIDUser.builder().email("Don´t care!").build();

    private Map<String, Object> testMap;

    @BeforeEach
    void setUp() {
      testMap = new LinkedHashMap<>();
    }

    @Test
    void userNotFound() {

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.patch(testId, testMap))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownField() {
      testMap.put("UNKNOWN_FIELD", "Don´t care");

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> testSubject.patch(testId, testMap))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void updateSuccessful() {
      var testFirstName = "Peter";
      var testLastName = "Zwegat";
      var testPhone = "555-1337424711";
      var testPassword = "testPassword";

      testMap.put("firstName", testFirstName);
      testMap.put("lastName", testLastName);
      testMap.put("phone", testPhone);
      testMap.put("password", testPassword);

      var testEncodedPassword = "BANANARAMA";

      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));

      testUser.setPassword(testPassword);
      when(userRepositoryMock.save(testUser))
          .thenAnswer(
              invocation -> {
                TestUUIDUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);
                assertThat(toSave.getFirstName()).isEqualTo(testFirstName);
                assertThat(toSave.getLastName()).isEqualTo(testLastName);
                assertThat(toSave.getPhone()).isEqualTo(testPhone);

                return toSave;
              });

      assertDoesNotThrow(() -> testSubject.patch(testId, testMap));
    }
  }

  @Nested
  class GetCurrentLoggedInUser {
    @Test
    void noUserLoggedIn() {
      assertThatThrownBy(() -> testSubject.getUserFromPrincipal(null))
          .isInstanceOf(SessionAuthenticationException.class);
    }

    @Test
    void userWronglyLoggedIn() {
      var testPrincipal = mock(UsernamePasswordAuthenticationToken.class);

      when(testPrincipal.getPrincipal()).thenReturn(null);

      assertThatThrownBy(() -> testSubject.getUserFromPrincipal(testPrincipal))
          .isInstanceOf(SessionAuthenticationException.class);
    }

    @Test
    void userIsLoggedIn() {
      var testPrincipal = mock(UsernamePasswordAuthenticationToken.class);
      var testUser = mock(TestUUIDUser.class);

      when(testPrincipal.getPrincipal()).thenReturn(testUser);

      Assertions.assertThat(testSubject.getUserFromPrincipal(testPrincipal)).isSameAs(testUser);
    }
  }

  @Nested
  class LoadUserByUsername {

    private final String testUsername = "test@example.com";

    @Test
    void userPresent() {
      var mockUserResponse = mock(TestUUIDUser.class);

      when(userRepositoryMock.findByEmailIgnoreCase(testUsername))
          .thenReturn(Optional.of(mockUserResponse));

      Assertions.assertThat(testSubject.loadUserByUsername(testUsername))
          .isSameAs(mockUserResponse);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findByEmailIgnoreCase(testUsername)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.loadUserByUsername(testUsername))
          .isInstanceOf(UsernameNotFoundException.class);
    }
  }

  @Test
  void deleteUserById() {
    when(userRepositoryMock.existsById(testId)).thenReturn(true);
    doNothing().when(userRepositoryMock).deleteById(testId);

    testSubject.deleteById(testId);

    verify(userRepositoryMock).deleteById(testId);
  }

  @Nested
  class CreateResetPasswordToken {

    private final String testUsername = "test@example.com";

    @Test
    void successful() throws CheckedMailException {
      TestUUIDUser testUser = mock(TestUUIDUser.class);
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

      var savedUser = mock(TestUUIDUser.class);
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

      testSubject.createResetPasswordToken(testUsername);
    }

    @Test
    void userNotFound() {
      when(userRepositoryMock.findByEmailIgnoreCase(testUsername)).thenReturn(Optional.empty());
      assertThrows(
          UsernameNotFoundException.class,
          () -> testSubject.createResetPasswordToken(testUsername));
      verify(userRepositoryMock, times(1)).findByEmailIgnoreCase(anyString());
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Nested
  class SelfUpdate {
    private final TestUUIDUser testUser = TestUUIDUser.builder().email(TEST_USERNAME).build();
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
      assertThatThrownBy(() -> testSubject.selfUpdate(null, new UserDto<>()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUpdateUserByDto() {
      final var NEW_FIRST_NAME = "Robin";
      final var NEW_LAST_NAME = "The Ripper";
      final var NEW_PHONE = "018012345";
      final var NEW_MOBILE = "018098765";
      final var NEW_LOCALE = Locale.ITALY;
      final var NEW_PASSWORD = "hopefully not working!";

      final UserDto updates = new UserDto<>();
      updates.setFirstName(NEW_FIRST_NAME);
      updates.setLastName(NEW_LAST_NAME);
      updates.setPhone(NEW_PHONE);
      updates.setMobile(NEW_MOBILE);
      updates.setLocale(NEW_LOCALE);
      updates.setPassword(NEW_PASSWORD);

      when(userRepositoryMock.save(any(TestUUIDUser.class))).thenAnswer(c -> c.getArgument(0));

      final var result =
          testSubject.selfUpdate((TestUUIDUser) testPrincipal.getPrincipal(), updates);
      assertThat(result.getFirstName()).isEqualTo(NEW_FIRST_NAME);
      assertThat(result.getLastName()).isEqualTo(NEW_LAST_NAME);
      assertThat(result.getPhone()).isEqualTo(NEW_PHONE);
      assertThat(result.getMobile()).isEqualTo(NEW_MOBILE);
      assertThat(result.getLocale()).isEqualTo(NEW_LOCALE);
      assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD_HASH);

      verify(userRepositoryMock).save(any(TestUUIDUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdateUserByFields() {
      final var NEW_FIRST_NAME = "Robin";
      final var NEW_LAST_NAME = "The Ripper";
      final var NEW_PHONE = "018012345";
      final var NEW_MOBILE = "018098765";
      final var NEW_LOCALE = Locale.ITALY;
      final var NEW_PASSWORD = "hopefully not working!";

      final Map<String, Object> updates =
          Map.of(
              "firstName", NEW_FIRST_NAME,
              "lastName", NEW_LAST_NAME,
              "phone", NEW_PHONE,
              "mobile", NEW_MOBILE,
              "locale", NEW_LOCALE,
              "password", NEW_PASSWORD);

      when(userRepositoryMock.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
      when(userRepositoryMock.save(any(TestUUIDUser.class))).thenAnswer(c -> c.getArgument(0));

      final var result =
          testSubject.selfUpdate((TestUUIDUser) testPrincipal.getPrincipal(), updates);
      assertThat(result.getFirstName()).isEqualTo(NEW_FIRST_NAME);
      assertThat(result.getLastName()).isEqualTo(NEW_LAST_NAME);
      assertThat(result.getPhone()).isEqualTo(NEW_PHONE);
      assertThat(result.getMobile()).isEqualTo(NEW_MOBILE);
      assertThat(result.getLocale()).isEqualTo(NEW_LOCALE);
      assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD_HASH);

      verify(userRepositoryMock, times(2)).save(any(TestUUIDUser.class));
      verify(userRepositoryMock, times(2)).findById(any());
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordWrongCredentials() {
      final String NEW_PASSWORD = "secret password!";

      final var updateRequest = new PasswordUpdateRequest(NEW_PASSWORD, "wrong password");
      assertThrows(
          BadCredentialsException.class,
          () ->
              testSubject.updatePassword(
                  (TestUUIDUser) testPrincipal.getPrincipal(), updateRequest));

      verifyNoInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordSuccess() {
      final var NEW_PASSWORD_PLAIN = "secret password!";
      final var NEW_PASSWORD_HASH = "$2b$10$9yPzo9U15R1jqm6Db6Jg4uZTlvedRNii/orl4bQfY.nUBM5hL0/9a";

      when(passwordEncoderMock.matches(TEST_PASSWORD_PLAIN, TEST_PASSWORD_HASH)).thenReturn(true);
      when(passwordEncoderMock.encode(NEW_PASSWORD_PLAIN)).thenReturn(NEW_PASSWORD_HASH);
      when(userRepositoryMock.save(any(TestUUIDUser.class))).thenAnswer(c -> c.getArgument(0));
      when(userRepositoryMock.findById(any())).thenReturn(Optional.of(testUser));

      final var updateRequest = new PasswordUpdateRequest(NEW_PASSWORD_PLAIN, TEST_PASSWORD_PLAIN);
      final var result =
          testSubject.updatePassword((TestUUIDUser) testPrincipal.getPrincipal(), updateRequest);

      assertThat(result.getPassword()).isEqualTo(NEW_PASSWORD_HASH);
      assertThat(result.getNonce()).isNotEmpty();
      assertThat(result.getNonce()).isNotEqualTo(TEST_NONCE);

      verify(passwordEncoderMock).matches(anyString(), anyString());
      verify(passwordEncoderMock).encode(anyString());
      verify(userRepositoryMock).save(any(TestUUIDUser.class));
      verify(userRepositoryMock).findById(any());
      verifyNoMoreInteractions(userRepositoryMock);
    }

    @Test
    void testUpdatePasswordFailForExternalUser() {
      testUser.setSource("ldap");

      final var updateRequest = new PasswordUpdateRequest("shouldbeignored", TEST_PASSWORD_PLAIN);

      assertThrows(
          NotAllowedException.class,
          () ->
              testSubject.updatePassword(
                  (TestUUIDUser) testPrincipal.getPrincipal(), updateRequest));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }
}
