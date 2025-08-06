package de.frachtwerk.essencium.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.frachtwerk.essencium.backend.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@DisplayName("Interact with the UserService")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock UserRepository userRepositoryMock;

  @InjectMocks private UserService testSubject;

  @Test
  @DisplayName("Fetch all Users as page sorted by custom name parameter")
  void getAllOrderByName() {
    Page<?> page = mock(Page.class);
    Pageable pageableWithSort = PageRequest.of(0, 20, Direction.DESC, "name");

    doReturn(page).when(page).map(any());
    doReturn(page).when(userRepositoryMock).findAll(any(Pageable.class));

    testSubject.getAll(pageableWithSort);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(userRepositoryMock, times(1)).findAll(pageableCaptor.capture());

    Sort expectedSort = Sort.by(Direction.DESC, "firstName", "lastName");
    Pageable capturedPageable = pageableCaptor.getValue();
    Assertions.assertThat(capturedPageable.getSort()).isEqualTo(expectedSort);
  }
}
