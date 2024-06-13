package de.frachtwerk.essencium.backend.api.data.role;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import java.util.Arrays;
import java.util.HashSet;

public class TestObjectsRole {
  public Role defaultRole() {
    return Role.builder().name("ROLE").description("ROLE").build();
  }

  public Role roleWithNameAndDescription(String name) {
    return Role.builder().name(name).description(name).build();
  }

  public Role roleWithRights(Right... rights) {
    Role role = defaultRole();
    role.setRights(new HashSet<>(Arrays.asList(rights)));
    return role;
  }
}
