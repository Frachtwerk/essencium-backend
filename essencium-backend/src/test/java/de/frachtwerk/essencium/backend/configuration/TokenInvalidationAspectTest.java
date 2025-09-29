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

package de.frachtwerk.essencium.backend.configuration;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.SessionTokenInvalidationService;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TokenInvalidationAspectTest {

  private final SessionTokenInvalidationService sessionTokenInvalidationServiceMock =
      Mockito.mock(SessionTokenInvalidationService.class);

  private final UserTokenInvalidationAspect testSubject =
      new UserTokenInvalidationAspect(sessionTokenInvalidationServiceMock);

  @Test
  void aroundUserModificationWithSingleUser() throws Throwable {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    AbstractBaseUser userMock = Mockito.mock(AbstractBaseUser.class);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {userMock});
    when(userMock.getUsername()).thenReturn("testuser@example.com");
    when(userMock.getId()).thenReturn("1L");

    testSubject.beforeUserModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(userMock);
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void aroundUserModificationWithUserCollection() throws Throwable {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    AbstractBaseUser user1Mock = Mockito.mock(AbstractBaseUser.class);
    AbstractBaseUser user2Mock = Mockito.mock(AbstractBaseUser.class);
    List<AbstractBaseUser> users = Arrays.asList(user1Mock, user2Mock);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {users});
    when(user1Mock.getUsername()).thenReturn("user1@example.com");
    when(user2Mock.getUsername()).thenReturn("user2@example.com");
    when(user1Mock.getId()).thenReturn("1L");
    when(user2Mock.getId()).thenReturn("2L");
    testSubject.beforeUserModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(user1Mock);
    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(user2Mock);
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void aroundUserModificationWithNoArgs() throws Throwable {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {});

    testSubject.beforeUserModification(ProceedingJoinPointMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRoleModificationWithSingleRole() {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    Role roleMock = Mockito.mock(Role.class);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {roleMock});
    when(roleMock.getName()).thenReturn("ADMIN");

    testSubject.beforeRoleModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("ADMIN");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRoleModificationWithRoleCollection() {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    Role role1Mock = Mockito.mock(Role.class);
    Role role2Mock = Mockito.mock(Role.class);
    List<Role> roles = Arrays.asList(role1Mock, role2Mock);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {roles});
    when(role1Mock.getName()).thenReturn("ADMIN");
    when(role2Mock.getName()).thenReturn("USER");

    testSubject.beforeRoleModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("ADMIN");
    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("USER");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRightModificationWithSingleRight() {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    Right rightMock = Mockito.mock(Right.class);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {rightMock});
    when(rightMock.getAuthority()).thenReturn("READ_PRIVILEGE");

    testSubject.beforeRightModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("READ_PRIVILEGE");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRightModificationWithRightCollection() {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    Right right1Mock = Mockito.mock(Right.class);
    Right right2Mock = Mockito.mock(Right.class);
    List<Right> rights = Arrays.asList(right1Mock, right2Mock);

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {rights});
    when(right1Mock.getAuthority()).thenReturn("READ_PRIVILEGE");
    when(right2Mock.getAuthority()).thenReturn("WRITE_PRIVILEGE");

    testSubject.beforeRightModification(ProceedingJoinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("READ_PRIVILEGE");
    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("WRITE_PRIVILEGE");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRoleWithValidRole() {
    Role roleMock = Mockito.mock(Role.class);
    when(roleMock.getName()).thenReturn("ADMIN");

    testSubject.invalidateUsersByRole(roleMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("ADMIN");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRoleWithNullRole() {
    testSubject.invalidateUsersByRole(null);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRoleWithNullRoleName() {
    Role roleMock = Mockito.mock(Role.class);
    when(roleMock.getName()).thenReturn(null);

    testSubject.invalidateUsersByRole(roleMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRightWithValidRight() {
    Right rightMock = Mockito.mock(Right.class);
    when(rightMock.getAuthority()).thenReturn("READ_PRIVILEGE");

    testSubject.invalidateUsersByRight(rightMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("READ_PRIVILEGE");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRightWithNullRight() {
    testSubject.invalidateUsersByRight(null);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUsersByRightWithNullAuthority() {
    Right rightMock = Mockito.mock(Right.class);
    when(rightMock.getAuthority()).thenReturn(null);

    testSubject.invalidateUsersByRight(rightMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void extractEntitiesWithUnexpectedType() throws Throwable {
    ProceedingJoinPoint ProceedingJoinPointMock = Mockito.mock(ProceedingJoinPoint.class);
    String unexpectedArg = "not a user";

    when(ProceedingJoinPointMock.getArgs()).thenReturn(new Object[] {unexpectedArg});
    when(ProceedingJoinPointMock.getSignature()).thenReturn(Mockito.mock(Signature.class));
    when(ProceedingJoinPointMock.getSignature().getName()).thenReturn("testMethod");

    testSubject.beforeUserModification(ProceedingJoinPointMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }
}
