package de.frachtwerk.essencium.backend.api.data;

import de.frachtwerk.essencium.backend.api.data.role.TestObjectsRole;
import de.frachtwerk.essencium.backend.api.data.service.TestObjectService;
import de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser;

public class TestObjects {

  public static TestObjectsUser users() {
    return new TestObjectsUser();
  }

  public static TestObjectService services() {
    return new TestObjectService();
  }

  public static TestObjectsRole roles() {
    return new TestObjectsRole();
  }
}
