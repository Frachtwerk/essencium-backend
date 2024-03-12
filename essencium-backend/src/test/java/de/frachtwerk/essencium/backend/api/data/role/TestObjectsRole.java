package de.frachtwerk.essencium.backend.api.data.role;

import de.frachtwerk.essencium.backend.model.Role;

public class TestObjectsRole {
  public Role defaultRole() {
    return Role.builder().name("ROLE").description("ROLE").build();
  }

  public Role roleWithNameAndDescription(String name) {
    return Role.builder().name(name).description(name).build();
  }
}
