package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.service.RightService;

public class RightServiceMockConfiguration implements MockConfiguration {

  protected final RightService mockedObject;

  public RightServiceMockConfiguration(RightService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public RightServiceMockConfiguration returnRightOnFindByAuthorityFor(String authority) {
    doReturn(TestObjects.rights().rightWithAuthorityAndDescription(authority))
        .when(mockedObject)
        .findByAuthority(authority);

    return this;
  }
}
