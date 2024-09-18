package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.service.RightService;

public class RightServiceMockConfiguration implements MockConfiguration {

  private final RightService mockedObject;

  public RightServiceMockConfiguration(RightService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public RightServiceMockConfiguration returnPassedObjectOnFindByAuthority() {
    doAnswer(
            invocationOnMock -> {
              if (invocationOnMock.getArgument(0) == null) {
                return null;
              }
              return Right.builder().authority(invocationOnMock.getArgument(0)).build();
            })
        .when(mockedObject)
        .findByAuthority(anyString());
    return this;
  }
}
