package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.verifyNoInteractions;

import de.frachtwerk.essencium.backend.service.UserEmailChangeService;
import java.io.Serializable;
import org.assertj.core.api.AbstractAssert;

public class UserEmailChangeServiceAssert<I extends Serializable>
    extends AbstractAssert<UserEmailChangeServiceAssert<I>, UserEmailChangeService<?, I>> {
  public UserEmailChangeServiceAssert(UserEmailChangeService<?, I> actual) {
    super(actual, UserEmailChangeServiceAssert.class);
  }

  public void hasNoMoreInteractions() {
    verifyNoInteractions(actual);
  }
}
