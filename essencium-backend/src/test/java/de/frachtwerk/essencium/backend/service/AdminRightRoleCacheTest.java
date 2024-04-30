package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Interact with the AdminRightRoleCacheTest")
@ExtendWith(MockitoExtension.class)
class AdminRightRoleCacheTest {

  @Mock private RightRepository rightRepository;
  @Mock private RoleRepository roleRepository;

  @InjectMocks private AdminRightRoleCache underTest;

  @Test
  @DisplayName("Calling reset should clear the caches")
  void reset() {
    Right adminRight = new Right();
    Role adminRole = new Role();

    adminRole.setRights(Set.of(adminRight));

    doReturn(List.of(adminRole)).when(roleRepository).findAll();
    doReturn(Optional.of(adminRight)).when(rightRepository).findById(anyString());

    underTest.getAdminRoles();
    assertThat(underTest.isEmpty()).isFalse();

    underTest.reset();

    assertThat(underTest.isEmpty()).isTrue();
  }
}
