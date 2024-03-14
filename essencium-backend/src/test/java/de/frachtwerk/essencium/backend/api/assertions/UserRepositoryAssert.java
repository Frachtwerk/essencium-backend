package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import java.io.Serializable;

public class UserRepositoryAssert<I extends Serializable> extends RepositoryAssert<I> {
  protected UserRepositoryAssert(BaseRepository<?, I> actual) {
    super(actual);
  }

  public void invokedFindByEmailIgnoreCaseOneTimeFor(String email) {
    verify((BaseUserRepository<?, I>) actual).findByEmailIgnoreCase(email);
  }
}
