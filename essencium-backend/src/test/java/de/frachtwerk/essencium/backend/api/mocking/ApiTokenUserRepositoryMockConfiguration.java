package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import de.frachtwerk.essencium.backend.repository.ApiTokenUserRepository;
import de.frachtwerk.essencium.backend.repository.specification.ApiTokenUserSpecification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ApiTokenUserRepositoryMockConfiguration implements MockConfiguration {

  protected final ApiTokenUserRepository mockedObject;

  public ApiTokenUserRepositoryMockConfiguration(ApiTokenUserRepository mockedObject) {
    this.mockedObject = mockedObject;
  }

  public ApiTokenUserRepositoryMockConfiguration existsById() {
    doReturn(true).when(mockedObject).existsById(any(UUID.class));

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration notExistsById() {
    doReturn(false).when(mockedObject).existsById(any(UUID.class));

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration returnAlwaysPassedObjectOnSave() {
    doAnswer(returnsFirstArg()).when(mockedObject).save(any());
    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration existsByLinkedUserAndDescription() {
    doReturn(true).when(mockedObject).existsByLinkedUserAndDescription(anyString(), anyString());

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration notExistsByLinkedUserAndDescription() {
    doReturn(false).when(mockedObject).existsByLinkedUserAndDescription(anyString(), anyString());

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration returnOnFindAll(ApiTokenUser returnValue) {
    doReturn(new PageImpl<>(List.of(returnValue)))
        .when(mockedObject)
        .findAll(any(ApiTokenUserSpecification.class), any(Pageable.class));

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration returnOnFindByIdFor(
      UUID id, ApiTokenUser returnValue) {
    doReturn(Optional.of(returnValue)).when(mockedObject).findById(id);

    return this;
  }

  public ApiTokenUserRepositoryMockConfiguration returnEmptyOptionalOnFindByIdFor(UUID id) {
    doReturn(Optional.empty()).when(mockedObject).findById(id);

    return this;
  }
}
