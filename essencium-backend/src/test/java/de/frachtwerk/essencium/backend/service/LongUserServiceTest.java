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

import de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer;
import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.*;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@ExtendWith(MockitoExtension.class)
class LongUserServiceTest {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
  private final long testId = 42L;

  @Mock BaseUserRepository<TestLongUser, Long> userRepositoryMock;
  @Mock PasswordEncoder passwordEncoderMock;
  @Mock UserMailService userMailServiceMock;
  @Mock RoleService roleServiceMock;
  @Mock DefaultRoleInitializer roleInitializerMock;
  @Mock JwtTokenService jwtTokenServiceMock;

  LongUserService testSubject;

  @BeforeEach
  void setUp() {
    testSubject =
        new LongUserService(
            userRepositoryMock,
            passwordEncoderMock,
            userMailServiceMock,
            roleServiceMock,
            roleInitializerMock,
            jwtTokenServiceMock);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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

  private SecurityContext getSecurityContextMock(TestLongUser returnedUser) {
    SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
    Authentication authenticationMock = Mockito.mock(Authentication.class);

    Mockito.when(securityContextMock.getAuthentication()).thenReturn(authenticationMock);
    Mockito.when(authenticationMock.getPrincipal()).thenReturn(returnedUser);
    return securityContextMock;
  }

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
      var mockUserResponse = mock(TestLongUser.class);

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
          testSubject.createDefaultUser(
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

      final var mockResult = testSubject.create(testUser);

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

      testSubject.create(testUser);
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

      Assertions.assertThat(testSubject.create(testUser)).isSameAs(testSavedUser);
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
                      isA(TestLongUser.class),
                      hasProperty("email", Matchers.is(testUser.getEmail())),
                      hasProperty("source", Matchers.is(testUser.getSource())),
                      hasProperty("password", Matchers.nullValue()),
                      hasProperty("passwordResetToken", Matchers.nullValue()),
                      hasProperty("roles", Matchers.notNullValue())))))
          .thenReturn(mock(TestLongUser.class));

      testSubject.create(testUser);

      verify(userRepositoryMock, times(1)).save(any(TestLongUser.class));
      verifyNoInteractions(userMailServiceMock);
    }
  }

  @Nested
  class UpdateUser {
    private final UserDto userToUpdate = mock(UserDto.class);

    @Test
    void inconsistentId() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
      when(userToUpdate.getId()).thenReturn(testId + 42);

      assertThatThrownBy(() -> testSubject.update(testId, userToUpdate))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void userNotFound() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
      when(userToUpdate.getId()).thenReturn(testId);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.update(testId, userToUpdate))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatePassword() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
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

                // password is not saved as clear text
                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);

                return toSave;
              });
      assertDoesNotThrow(() -> testSubject.update(testId, userToUpdate));
    }

    @Test
    void rolesUpdateValid() {
      // build caller
      Role adminRole = Role.builder().name("ADMIN").description("ADMIN").build();
      Role userRole = Role.builder().name("USER").description("USER").build();
      Set<Role> callingRoles = new HashSet<>();
      callingRoles.add(adminRole);
      callingRoles.add(userRole);
      SecurityContextHolder.setContext(
          getSecurityContextMock(TestLongUser.builder().id(testId).roles(callingRoles).build()));

      // build update
      Set<Role> updateRoles = new HashSet<>();
      updateRoles.add(adminRole);
      updateRoles.add(userRole);
      when(userToUpdate.getId()).thenReturn(testId);
      when(userToUpdate.getRoles())
          .thenReturn(Set.of(adminRole.getAuthority(), userRole.getAuthority()));

      // build existing user
      final TestLongUser mockUser = mock(TestLongUser.class);
      when(mockUser.getSource()).thenReturn(AbstractBaseUser.USER_AUTH_SOURCE_LOCAL);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUser));
      when(roleServiceMock.getByName("ADMIN")).thenReturn(adminRole);
      when(roleServiceMock.getByName("USER")).thenReturn(userRole);
      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));

      when(userRepositoryMock.save(any(TestLongUser.class)))
          .thenAnswer(
              invocation -> {
                AbstractBaseUser toSave = invocation.getArgument(0);
                assertThat(toSave.getRoles()).isEmpty(); // first "save" does not contain any roles
                return toSave;
              })
          .thenAnswer(
              invocation -> {
                AbstractBaseUser toSave = invocation.getArgument(0);
                assertThat(toSave.getRoles()).containsAll(updateRoles);
                return toSave;
              });
      assertDoesNotThrow(() -> testSubject.update(testId, userToUpdate));
    }

    @Test
    void rolesUpdateInvalid() {
      // build caller
      Role adminRole = Role.builder().name("ADMIN").description("ADMIN").build();
      Role userRole = Role.builder().name("USER").description("USER").build();
      Set<Role> callingRoles = new HashSet<>();
      callingRoles.add(adminRole);
      callingRoles.add(userRole);
      SecurityContextHolder.setContext(
          getSecurityContextMock(TestLongUser.builder().id(testId).roles(callingRoles).build()));

      // build update
      when(userToUpdate.getId()).thenReturn(testId);
      when(userToUpdate.getRoles()).thenReturn(Set.of(userRole.getAuthority()));

      // build existing user
      final TestLongUser mockUser = mock(TestLongUser.class);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(mockUser));
      when(roleServiceMock.getDefaultRole()).thenReturn(mock(Role.class));
      lenient().when(roleInitializerMock.hasAdminRights(callingRoles)).thenReturn(true);
      lenient().when(roleInitializerMock.hasAdminRights(Set.of(userRole))).thenReturn(false);

      assertThatThrownBy(() -> testSubject.update(testId, userToUpdate))
          .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void testNoPasswordForExternalUser() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
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

      final TestLongUser savedUser = testSubject.update(TEST_USER_ID, userUpdate);
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
      // we should not be able to patch the password of a user sourced from oauth or ldap, as it
      // wouldn't make sense
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

      final TestLongUser savedUser = testSubject.patch(TEST_USER_ID, userUpdate);
      assertEquals(TEST_USER_ID, savedUser.getId());
      assertEquals(TEST_NONCE, savedUser.getNonce());
      assertNull(savedUser.getPassword());

      verify(userRepositoryMock, times(2)).findById(anyLong());
      verify(userRepositoryMock, times(2)).save(any(TestLongUser.class));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Nested
  class PatchUserFields {

    private final TestLongUser testUser = TestLongUser.builder().email("Don´t care!").build();

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
    void successful() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
      Role adminRole = Role.builder().name("ADMIN").description("ADMIN").build();
      Role userRole = Role.builder().name("USER").description("USER").build();

      String testFirstName = "Peter";
      String testLastName = "Zwegat";
      String testPhone = "555-1337424711";
      String testPassword = "testPassword";
      Set<Role> testRoles = new HashSet<>();
      testRoles.add(adminRole);
      testRoles.add(userRole);

      testMap.put("firstName", testFirstName);
      testMap.put("lastName", testLastName);
      testMap.put("phone", testPhone);
      testMap.put("password", testPassword);
      testMap.put("roles", testRoles);

      String testEncodedPassword = "BANANARAMA";

      when(passwordEncoderMock.encode(testPassword)).thenReturn(testEncodedPassword);
      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));
      when(roleServiceMock.getByName("ADMIN")).thenReturn(adminRole);
      when(roleServiceMock.getByName("USER")).thenReturn(userRole);

      testUser.setPassword(testPassword);
      when(userRepositoryMock.save(testUser))
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);
                assertThat(toSave.getRoles()).isEmpty(); // first "save" does not contain any roles
                return toSave;
              })
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);

                assertThat(toSave.getPassword()).isNotEqualTo(testPassword);
                assertThat(toSave.getPassword()).isEqualTo(testEncodedPassword);
                assertThat(toSave.getFirstName()).isEqualTo(testFirstName);
                assertThat(toSave.getLastName()).isEqualTo(testLastName);
                assertThat(toSave.getPhone()).isEqualTo(testPhone);
                assertThat(toSave.getRoles()).isEqualTo(testRoles);
                assertThat(toSave.getRoles()).containsAll(testRoles);

                return toSave;
              });
      assertDoesNotThrow(() -> testSubject.patch(testId, testMap));
    }

    @Test
    void rolesPatchValid() {
      Role adminRole = Role.builder().name("ADMIN").description("ADMIN").build();
      Role userRole = Role.builder().name("USER").description("USER").build();
      Set<Role> callingRoles = new HashSet<>();
      callingRoles.add(adminRole);
      callingRoles.add(userRole);
      SecurityContextHolder.setContext(
          getSecurityContextMock(TestLongUser.builder().id(testId).roles(callingRoles).build()));

      Set<Role> patchRoles = new HashSet<>();
      patchRoles.add(adminRole);
      // remove user role
      testMap.put("roles", patchRoles);

      when(userRepositoryMock.findById(testId)).thenReturn(Optional.of(testUser));
      when(roleServiceMock.getByName("ADMIN")).thenReturn(adminRole);
      when(userRepositoryMock.save(testUser))
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);
                assertThat(toSave.getRoles()).isEmpty(); // first "save" does not contain any roles
                return toSave;
              })
          .thenAnswer(
              invocation -> {
                TestLongUser toSave = invocation.getArgument(0);
                assertThat(toSave.getRoles()).isEqualTo(patchRoles);
                assertThat(toSave.getRoles()).containsAll(patchRoles);
                return toSave;
              });
      assertDoesNotThrow(() -> testSubject.patch(testId, testMap));
    }

    @Test
    void rolesPatchInvalid() {
      Role adminRole = Role.builder().name("ADMIN").description("ADMIN").build();
      Role userRole = Role.builder().name("USER").description("USER").build();
      Set<Role> callingRoles = new HashSet<>();
      callingRoles.add(adminRole);
      callingRoles.add(userRole);
      SecurityContextHolder.setContext(
          getSecurityContextMock(TestLongUser.builder().id(testId).roles(callingRoles).build()));

      Set<Role> patchRoles = new HashSet<>();
      // remove admin role
      patchRoles.add(userRole);
      testMap.put("roles", patchRoles);

      lenient().when(roleInitializerMock.hasAdminRights(callingRoles)).thenReturn(true);
      lenient().when(roleInitializerMock.hasAdminRights(patchRoles)).thenReturn(false);

      assertThatThrownBy(() -> testSubject.patch(testId, testMap))
          .isInstanceOf(NotAllowedException.class);
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
      var testUser = mock(TestLongUser.class);

      when(testPrincipal.getPrincipal()).thenReturn(testUser);

      Assertions.assertThat(testSubject.getUserFromPrincipal(testPrincipal)).isSameAs(testUser);
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

  @Nested
  class deleteUserById {

    @Test
    void successful() {
      SecurityContextHolder.setContext(getSecurityContextMock(TestLongUser.builder().build()));
      when(userRepositoryMock.existsById(testId)).thenReturn(true);
      doNothing().when(userRepositoryMock).deleteById(testId);

      testSubject.deleteById(testId);

      verify(userRepositoryMock).deleteById(testId);
    }

    @Test
    void cannotDeleteYourself() {
      SecurityContextHolder.setContext(
          getSecurityContextMock(TestLongUser.builder().id(testId).build()));
      when(userRepositoryMock.existsById(testId)).thenReturn(true);

      assertThrows(NotAllowedException.class, () -> testSubject.deleteById(testId));
    }
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
      assertThatThrownBy(() -> testSubject.selfUpdate(null, new UserDto<>()))
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
          testSubject.selfUpdate((TestLongUser) testPrincipal.getPrincipal(), updates);
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
          testSubject.selfUpdate((TestLongUser) testPrincipal.getPrincipal(), updates);
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
          () ->
              testSubject.updatePassword(
                  (TestLongUser) testPrincipal.getPrincipal(), updateRequest));

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
          testSubject.updatePassword((TestLongUser) testPrincipal.getPrincipal(), updateRequest);

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
          () ->
              testSubject.updatePassword(
                  (TestLongUser) testPrincipal.getPrincipal(), updateRequest));
      verifyNoMoreInteractions(userRepositoryMock);
    }
  }

  @Test
  void getTokensTest() {
    TestLongUser user = testSubject.convertDtoToEntity(testSubject.getNewUser());

    when(jwtTokenServiceMock.getTokens(user.getUsername()))
        .thenReturn(List.of(SessionToken.builder().build()));

    List<SessionToken> tokens = jwtTokenServiceMock.getTokens(user.getUsername());

    assertThat(tokens).isNotEmpty().hasSize(1);
    verify(jwtTokenServiceMock, times(1)).getTokens(user.getUsername());
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoInteractions(userRepositoryMock);
  }

  @Test
  void deleteToken() {
    TestLongUser user = testSubject.convertDtoToEntity(testSubject.getNewUser());
    UUID uuid = UUID.randomUUID();
    jwtTokenServiceMock.deleteToken(user.getUsername(), uuid);
    verify(jwtTokenServiceMock, times(1)).deleteToken(user.getUsername(), uuid);
    verifyNoMoreInteractions(jwtTokenServiceMock);
    verifyNoInteractions(userRepositoryMock);
  }
}
