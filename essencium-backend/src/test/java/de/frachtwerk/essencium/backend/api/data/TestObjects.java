package de.frachtwerk.essencium.backend.api.data;

import de.frachtwerk.essencium.backend.api.data.authentication.TestObjectsAuthentication;
import de.frachtwerk.essencium.backend.api.data.pageable.TestObjectsPageable;
import de.frachtwerk.essencium.backend.api.data.role.TestObjectsRole;
import de.frachtwerk.essencium.backend.api.data.service.TestObjectService;
import de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser;

/**
 * The utility class TestObjects serves as an easy way to receive often used "given" objects in test
 * cases.
 */
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

  public static TestObjectsAuthentication authentication() {
    return new TestObjectsAuthentication();
  }

  public static TestObjectsPageable pageable() {
    return new TestObjectsPageable();
  }
}
