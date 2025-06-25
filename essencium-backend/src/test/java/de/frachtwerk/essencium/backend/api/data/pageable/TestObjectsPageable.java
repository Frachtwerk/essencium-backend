package de.frachtwerk.essencium.backend.api.data.pageable;

import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class TestObjectsPageable {
  public Pageable mockedPageable() {
    return Mockito.mock(Pageable.class);
  }

  public Page<?> mockedPage() {
    return Mockito.mock(Page.class);
  }
}
