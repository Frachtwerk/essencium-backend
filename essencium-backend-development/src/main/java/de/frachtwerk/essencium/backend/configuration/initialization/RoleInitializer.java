package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.service.RightService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
public class RoleInitializer extends DefaultRoleInitializer {
  public RoleInitializer(
      RoleRepository roleRepository,
      InitProperties initProperties,
      RoleService roleService,
      RightService rightService) {
    super(roleRepository, initProperties, roleService, rightService);
  }

  @Override
  protected Collection<Role> getAdditionalRoles() {
    return List.of(
        Role.builder()
            .name("USER_2")
            .description("User 2")
            .rights(
                rightService.getAll().stream()
                    .filter(r -> r.getAuthority().startsWith("EXAMPLE_2"))
                    .collect(Collectors.toSet()))
            .build());
  }
}
