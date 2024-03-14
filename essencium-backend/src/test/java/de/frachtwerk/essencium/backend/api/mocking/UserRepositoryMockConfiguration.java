package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.io.Serializable;
import java.util.Optional;

public class UserRepositoryMockConfiguration<I extends Serializable>
    extends BaseRepositoryMockConfiguration<I> {
  public UserRepositoryMockConfiguration(BaseUserRepository<?, I> mockedObject) {
    super(mockedObject);
  }

  public UserRepositoryMockConfiguration<I> returnUserForGivenEmailIgnoreCase(
      String email, Object returnValue) {
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
}
