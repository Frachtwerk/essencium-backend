package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.service.RoleService;

public class RoleServiceMockConfiguration implements MockConfiguration {

  private final RoleService mockedObject;

  public RoleServiceMockConfiguration(RoleService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public RoleServiceMockConfiguration returnDefaultRoleOnDefaultRoleCall() {
    doReturn(TestObjects.roles().defaultRole()).when(mockedObject).getDefaultRole();

    return this;
  }

  public RoleServiceMockConfiguration returnRoleOnGetByNameFor(Role returnValue) {
    doReturn(returnValue).when(mockedObject).getByName(returnValue.getName());

    return this;
  }
}
