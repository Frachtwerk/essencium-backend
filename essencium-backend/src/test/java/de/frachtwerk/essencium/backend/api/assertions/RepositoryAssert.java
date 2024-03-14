package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.io.Serializable;
import org.assertj.core.api.AbstractAssert;

public class RepositoryAssert<I extends Serializable>
    extends AbstractAssert<RepositoryAssert<I>, BaseRepository<?, I>> {
  protected RepositoryAssert(BaseRepository<?, I> actual) {
    super(actual, RepositoryAssert.class);
  }

  public void invokedSaveOneTime() {
    invokedSaveNTimes(1);
  }

  public void invokedSaveNTimes(int expectedTimes) {
    verify(actual, times(expectedTimes)).save(any());
  }

  public void invokedFindByIdNTimes(int expectedTimes) {
    verify(actual, times(expectedTimes)).findById(any());
  }

  public void hasNoMoreInteractions() {
    verifyNoMoreInteractions(actual);
  }

  public void invokedDeleteByIdOneTime(I id) {
    verify(actual).deleteById(id);
  }
}
