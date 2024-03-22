package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.api.assertions.EssenciumAssertions.assertThat;
import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.TEST_NEW_EMAIL;
import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.configure;
import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.givenMocks;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.frachtwerk.essencium.backend.api.annotations.TestUserStub;
import de.frachtwerk.essencium.backend.api.annotations.TestUserStubType;
import de.frachtwerk.essencium.backend.api.annotations.UseTestObjects;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.dto.EmailVerificationRequest;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
@UseTestObjects
@DisplayName("Interact with the UserEmailChangeService")
public class UserEmailChangeServiceTest {

  @Mock BaseUserRepository<UserStub, Long> userRepositoryMock;

  @Mock UserMailService userMailServiceMock;

  private UserEmailChangeService<UserStub, Long> testSubject;

  @BeforeEach
  public void setUp() {
    testSubject = new UserEmailChangeService<>(userRepositoryMock, userMailServiceMock);
  }

  @Test
  @DisplayName("Should verify a to new email address with a valid token")
  void testVerifyEmailWithVerificationToken(
      @TestUserStub(type = TestUserStubType.VALIDATE_EMAIL) UserStub existingUser) {

    EmailVerificationRequest verificationRequest =
        new EmailVerificationRequest(existingUser.getEmailVerifyToken());

    givenMocks(
            configure(userRepositoryMock)
                .returnUserForGivenEmailVerificationToken(
                    verificationRequest.emailVerifyToken(), existingUser))
        .and(configure(userRepositoryMock).returnAlwaysPassedObjectOnSave());

    assertThat(existingUser).isNonNull().andHasANotVerifiedEmailState(TEST_NEW_EMAIL);

    testSubject.verifyEmailByToken(verificationRequest);

    assertThat(existingUser).isNonNull().andHasAVerifiedEmailStateFor(TEST_NEW_EMAIL);
  }

  @Test
  @DisplayName("Should throw a BadCredentialsException with an non existing token")
  void testVerifyEmailWithNonExistingVerificationToken(
      @TestUserStub(type = TestUserStubType.VALIDATE_EMAIL) UserStub existingUser) {
    EmailVerificationRequest verificationRequest = new EmailVerificationRequest(UUID.randomUUID());
    final String currentEmail = existingUser.getEmail();

    assertThat(existingUser).isNonNull().andHasANotVerifiedEmailState(TEST_NEW_EMAIL);

    assertThrows(
        BadCredentialsException.class,
        () -> testSubject.verifyEmailByToken(verificationRequest),
        "Invalid verification token");

    assertThat(existingUser).isNonNull().andHasEmail(currentEmail);
    assertThat(existingUser).isNonNull().andHasANotVerifiedEmailState(TEST_NEW_EMAIL);
  }

  @Test
  @DisplayName("Should throw a BadCredentialsException with an expired token")
  void testVerifyEmailWithExpiredVerificationToken(
      @TestUserStub(type = TestUserStubType.VALIDATE_EMAIL) UserStub existingUser) {

    EmailVerificationRequest verificationRequest =
        new EmailVerificationRequest(existingUser.getEmailVerifyToken());
    existingUser.setEmailVerificationTokenExpiringAt(LocalDateTime.now().minusMinutes(1));
    final String currentEmail = existingUser.getEmail();

    assertThat(existingUser).isNonNull().andHasANotVerifiedEmailState(TEST_NEW_EMAIL);

    assertThrows(
        BadCredentialsException.class,
        () -> testSubject.verifyEmailByToken(verificationRequest),
        "Verification token expired");

    assertThat(existingUser).isNonNull().andHasEmail(currentEmail);
    assertThat(existingUser).isNonNull().andHasANotVerifiedEmailState(TEST_NEW_EMAIL);
  }
}
