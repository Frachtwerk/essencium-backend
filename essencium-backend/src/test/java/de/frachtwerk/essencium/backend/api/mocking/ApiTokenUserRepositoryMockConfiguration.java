package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.repository.ApiTokenUserRepository;
import java.util.Optional;
import java.util.UUID;

public class ApiTokenUserRepositoryMockConfiguration implements MockConfiguration {

  private final ApiTokenUserRepository mockedObject;

  public ApiTokenUserRepositoryMockConfiguration(ApiTokenUserRepository apiTokenUserRepository) {
    this.mockedObject = apiTokenUserRepository;
  }

  public ApiTokenUserRepositoryMockConfiguration returnOnFindByIdFor(UUID id, Object returnValue) {
    doReturn(Optional.ofNullable(returnValue)).when(mockedObject).findById(id);

    return this;
  }
}
