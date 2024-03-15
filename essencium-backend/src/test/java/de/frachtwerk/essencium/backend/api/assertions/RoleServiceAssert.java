package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.service.RoleService;
import org.assertj.core.api.AbstractAssert;

public class RoleServiceAssert extends AbstractAssert<RoleServiceAssert, RoleService> {
  protected RoleServiceAssert(RoleService actual) {
    super(actual, RoleServiceAssert.class);
  }

  public void invokedNeverGetByNameFor(String roleName) {
    invokedGetByNameNTimesFor(0, roleName);
  }

  public void invokedGetByNameNTimesFor(int invokedTimes, String roleName) {
    verify(actual, times(invokedTimes)).getByName(roleName);
  }
}
