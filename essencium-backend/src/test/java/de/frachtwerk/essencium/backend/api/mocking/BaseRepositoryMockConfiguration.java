package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.io.Serializable;
import java.util.Optional;

public class BaseRepositoryMockConfiguration<I extends Serializable> implements MockConfiguration {

  protected final BaseRepository<?, I> mockedObject;

  public BaseRepositoryMockConfiguration(BaseRepository<?, I> mockedObject) {
    this.mockedObject = mockedObject;
  }

  public BaseRepositoryMockConfiguration<I> returnAlwaysPassedObjectOnSave() {
    doAnswer(returnsFirstArg()).when(mockedObject).save(any());
    return this;
  }

  public BaseRepositoryMockConfiguration<I> returnOnFindByIdFor(I id, Object returnValue) {
    doReturn(Optional.of(returnValue)).when(mockedObject).findById(id);

    return this;
  }

  public BaseRepositoryMockConfiguration<I> entityWithIdExists(I id) {
    doReturn(true).when(mockedObject).existsById(id);

    return this;
  }

  public BaseRepositoryMockConfiguration<I> doNothingOnDeleteEntityWithId(I id) {
    doNothing().when(mockedObject).deleteById(id);

    return this;
  }
}
