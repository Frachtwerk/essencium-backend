package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryMockConfiguration<I extends Serializable>
    extends BaseRepositoryMockConfiguration<I> {
  public UserRepositoryMockConfiguration(BaseUserRepository<?, I> mockedObject) {
    super(mockedObject);
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenEmailIgnoreCase(
      String email, AbstractBaseUser<I> returnValue) {
    doReturn(Optional.of(returnValue))
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByEmailIgnoreCase(email);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnNoUserForGivenEmailIgnoreCase(String email) {
    doReturn(Optional.empty())
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByEmailIgnoreCase(email);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenPasswordResetToken(
      String token, AbstractBaseUser<I> returnValue) {
    doReturn(Optional.of(returnValue))
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByPasswordResetToken(token);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnNoUserForGivenPasswordResetToken(String token) {
    doReturn(Optional.empty())
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByPasswordResetToken(token);

    return this;
  }

  public UserRepositoryMockConfiguration<I> anotherAdminExistsInTheSystem() {
    doReturn(true)
        .when((BaseUserRepository<?, I>) mockedObject)
        .existsAnyAdminBesidesUserWithId(any(), any());

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenEmailVerificationToken(
      UUID token, AbstractBaseUser<I> returnValue) {
    doReturn(Optional.of(returnValue))
        .when((BaseUserRepository<?, I>) mockedObject)
        .findByEmailVerifyToken(token);

    return this;
  }

  public UserRepositoryMockConfiguration<I> returnTrueOnExistsCallForEmail(String email) {
    doReturn(true).when((BaseUserRepository<?, I>) mockedObject).existsByEmailIgnoreCase(email);

    return this;
  }
}
