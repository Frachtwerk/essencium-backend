package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AdminRightRoleCache {

  private final Set<Role> adminRoles = new HashSet<>();

  private final Set<Right> adminRights = new HashSet<>();

  private final RightRepository rightRepository;
  private final RoleRepository roleRepository;

  public AdminRightRoleCache(RightRepository rightRepository, RoleRepository roleRepository) {
    this.rightRepository = rightRepository;
    this.roleRepository = roleRepository;
  }

  public Set<Role> getAdminRoles() {
    if (adminRoles.isEmpty()) {
      adminRoles.addAll(
          roleRepository.findAll().stream()
              .filter(role -> role.getRights().containsAll(getAdminRights()))
              .collect(Collectors.toSet()));
    }

    return Set.copyOf(adminRoles);
  }

  public Set<Right> getAdminRights() {
    if (adminRights.isEmpty()) {
      adminRights.addAll(
          // admin rights will be reset to BasicApplicationRights on every startup
          Arrays.stream(BasicApplicationRight.values())
              .map(BasicApplicationRight::getAuthority)
              .map(rightRepository::findById)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toCollection(HashSet::new)));
    }

    return Set.copyOf(adminRights);
  }

  public void reset() {
    this.adminRoles.clear();
    this.adminRights.clear();
  }

  public boolean isEmpty() {
    return this.adminRoles.isEmpty() && this.adminRights.isEmpty();
  }
}
