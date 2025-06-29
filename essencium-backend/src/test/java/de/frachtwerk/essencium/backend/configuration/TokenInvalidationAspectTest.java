package de.frachtwerk.essencium.backend.configuration;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.SessionTokenInvalidationService;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TokenInvalidationAspectTest {

  private final SessionTokenInvalidationService sessionTokenInvalidationServiceMock =
      Mockito.mock(SessionTokenInvalidationService.class);

  private final UserTokenInvalidationAspect testSubject =
      new UserTokenInvalidationAspect(sessionTokenInvalidationServiceMock);

  @Test
  void ignoreInitializerMethods() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    Signature signatureMock = Mockito.mock(Signature.class);

    when(joinPointMock.getSignature()).thenReturn(signatureMock);
    when(signatureMock.getName()).thenReturn("testMethod");

    testSubject.ignoreInitializerMethods(joinPointMock);

    verify(joinPointMock).getSignature();
    verify(signatureMock).getName();
    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeUserModificationWithSingleUser() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    AbstractBaseUser userMock = Mockito.mock(AbstractBaseUser.class);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {userMock});
    when(userMock.getUsername()).thenReturn("testuser@example.com");

    testSubject.beforeUserModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(userMock);
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeUserModificationWithUserCollection() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    AbstractBaseUser user1Mock = Mockito.mock(AbstractBaseUser.class);
    AbstractBaseUser user2Mock = Mockito.mock(AbstractBaseUser.class);
    List<AbstractBaseUser> users = Arrays.asList(user1Mock, user2Mock);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {users});
    when(user1Mock.getUsername()).thenReturn("user1@example.com");
    when(user2Mock.getUsername()).thenReturn("user2@example.com");

    testSubject.beforeUserModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(user1Mock);
    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(user2Mock);
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeUserModificationWithNoArgs() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {});

    testSubject.beforeUserModification(joinPointMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRoleModificationWithSingleRole() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    Role roleMock = Mockito.mock(Role.class);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {roleMock});
    when(roleMock.getName()).thenReturn("ADMIN");

    testSubject.beforeRoleModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("ADMIN");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRoleModificationWithRoleCollection() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    Role role1Mock = Mockito.mock(Role.class);
    Role role2Mock = Mockito.mock(Role.class);
    List<Role> roles = Arrays.asList(role1Mock, role2Mock);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {roles});
    when(role1Mock.getName()).thenReturn("ADMIN");
    when(role2Mock.getName()).thenReturn("USER");

    testSubject.beforeRoleModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("ADMIN");
    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRole("USER");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRightModificationWithSingleRight() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    Right rightMock = Mockito.mock(Right.class);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {rightMock});
    when(rightMock.getAuthority()).thenReturn("READ_PRIVILEGE");

    testSubject.beforeRightModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("READ_PRIVILEGE");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void beforeRightModificationWithRightCollection() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    Right right1Mock = Mockito.mock(Right.class);
    Right right2Mock = Mockito.mock(Right.class);
    List<Right> rights = Arrays.asList(right1Mock, right2Mock);

    when(joinPointMock.getArgs()).thenReturn(new Object[] {rights});
    when(right1Mock.getAuthority()).thenReturn("READ_PRIVILEGE");
    when(right2Mock.getAuthority()).thenReturn("WRITE_PRIVILEGE");

    testSubject.beforeRightModification(joinPointMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("READ_PRIVILEGE");
    verify(sessionTokenInvalidationServiceMock).invalidateTokensForRight("WRITE_PRIVILEGE");
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUserTokensWithValidUser() {
    AbstractBaseUser userMock = Mockito.mock(AbstractBaseUser.class);
    when(userMock.getUsername()).thenReturn("testuser@example.com");

    testSubject.invalidateUserTokens(userMock);

    verify(sessionTokenInvalidationServiceMock).invalidateTokensOnUserUpdate(userMock);
    verifyNoMoreInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUserTokensWithNullUser() {
    testSubject.invalidateUserTokens(null);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }

  @Test
  void invalidateUserTokensWithNullUsername() {
    AbstractBaseUser userMock = Mockito.mock(AbstractBaseUser.class);
    when(userMock.getUsername()).thenReturn(null);

    testSubject.invalidateUserTokens(userMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
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
  void extractEntitiesWithUnexpectedType() {
    JoinPoint joinPointMock = Mockito.mock(JoinPoint.class);
    String unexpectedArg = "not a user";

    when(joinPointMock.getArgs()).thenReturn(new Object[] {unexpectedArg});
    when(joinPointMock.getSignature()).thenReturn(Mockito.mock(Signature.class));
    when(joinPointMock.getSignature().getName()).thenReturn("testMethod");

    testSubject.beforeUserModification(joinPointMock);

    verifyNoInteractions(sessionTokenInvalidationServiceMock);
  }
}
