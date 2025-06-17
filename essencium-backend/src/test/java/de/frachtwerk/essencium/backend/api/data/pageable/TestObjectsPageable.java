package de.frachtwerk.essencium.backend.api.data.pageable;

import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class TestObjectsPageable {
  public Pageable mockedPageable() {
    Sort mockSort = Mockito.mock(Sort.class);
    Pageable mockPageable = Mockito.mock(Pageable.class);

    Mockito.when(mockPageable.getSort()).thenReturn(mockSort);
    return mockPageable;
  }

  public Page<?> mockedPage() {
    return Mockito.mock(Page.class);
  }
}
