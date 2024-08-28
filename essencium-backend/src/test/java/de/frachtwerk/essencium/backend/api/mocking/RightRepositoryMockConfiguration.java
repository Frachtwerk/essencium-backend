package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public class RightRepositoryMockConfiguration implements MockConfiguration {
  protected final RightRepository mockedObject;

  public RightRepositoryMockConfiguration(RightRepository mockedObject) {
    this.mockedObject = mockedObject;
  }

  public RightRepositoryMockConfiguration returnsOnFindAll(Iterable<Right> returnValue) {
    doReturn(returnValue).when(mockedObject).findAll();
    return this;
  }

  public RightRepositoryMockConfiguration returnAlwaysPassedObjectOnSave() {
    doAnswer(returnsFirstArg()).when(mockedObject).save(any());
    return this;
  }

  public RightRepositoryMockConfiguration doNothingOnDeleteEntityByAuthority(String authority) {
    doNothing().when(mockedObject).deleteByAuthority(authority);

    return this;
  }

  public RightRepositoryMockConfiguration returnOnFindByIdFor(
      @NotNull String authority, Right right) {
    doReturn(Optional.ofNullable(right)).when(mockedObject).findById(authority);

    return this;
  }
}
