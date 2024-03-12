package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import org.assertj.core.api.AbstractAssert;

public class RepositoryAssert extends AbstractAssert<RepositoryAssert, BaseRepository<?, ?>> {
  protected RepositoryAssert(BaseRepository actual) {
    super(actual, RepositoryAssert.class);
  }

  public void invokedSaveOneTime() {
    invokedSaveNTimes(1);
  }

  public void invokedSaveNTimes(int expectedTimes) {
    verify(actual, times(expectedTimes)).save(any());
  }
}
