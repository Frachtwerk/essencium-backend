package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.*;

public class TestRoleInitializer extends DefaultRoleInitializer {

  private final boolean defaultRole;

  public TestRoleInitializer(
      InitProperties initProperties,
      RoleRepository roleRepository,
      RoleService roleService,
      RightService rightService,
      boolean defaultRole) {
    super(initProperties, roleRepository, roleService, rightService);
    this.defaultRole = defaultRole;
  }

  @Override
  protected Collection<Role> getAdditionalRoles() {
    HashSet<Right> rights = new HashSet<>(List.of(Right.builder().authority("right").build()));
    return new ArrayList<>(
        List.of(
            Role.builder()
                .name("TEST")
                .description("TEST")
                .isDefaultRole(defaultRole)
                .rights(rights)
                .build(),
            Role.builder().name("USER").description("User").rights(rights).build()));
  }
}
