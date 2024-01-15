package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TestRoleInitializer extends DefaultRoleInitializer {

  public TestRoleInitializer(
      RoleService roleService,
      RoleRepository roleRepository,
      RightService rightService,
      InitProperties initProperties) {
    super(roleService, roleRepository, rightService, initProperties);
  }

  @Override
  protected Collection<Role> getAdditionalRoles() {
    return List.of(
        Role.builder().name("TEST").description("TEST").rights(Set.of()).build(),
        Role.builder().name("USER").description("User").rights(Set.of()).build());
  }
}
