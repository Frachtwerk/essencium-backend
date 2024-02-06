package de.frachtwerk.essencium.backend.model.dto;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleDtoTest {

  @Test
  void testCreateRoleWithMap() {
    RoleDto roleDto =
        RoleDto.builder()
            .name("MAP_ROLE")
            .description("MAP_ROLE")
            .rights(
                Set.of(
                    Map.of("authority", "RIGHT_1"),
                    Map.of("authority", "RIGHT_2", "description", "description")))
            .build();
    Role role = roleDto.toRole();
    assertThat(role.getName()).isEqualTo("MAP_ROLE");
    assertThat(role.getDescription()).isEqualTo("MAP_ROLE");
    assertThat(role.getRights()).hasSize(2);
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right -> right.getAuthority().equals("RIGHT_1") && right.getDescription() == null));
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right ->
                    right.getAuthority().equals("RIGHT_2")
                        && right.getDescription().equals("description")));
  }

  @Test
  void testCreateRoleWithStrings() {
    RoleDto roleDto =
        RoleDto.builder()
            .name("MAP_ROLE")
            .description("MAP_ROLE")
            .rights(Set.of("RIGHT_1", "RIGHT_2"))
            .build();
    Role role = roleDto.toRole();
    assertThat(role.getName()).isEqualTo("MAP_ROLE");
    assertThat(role.getDescription()).isEqualTo("MAP_ROLE");
    assertThat(role.getRights()).hasSize(2);
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right -> right.getAuthority().equals("RIGHT_1") && right.getDescription() == null));
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right -> right.getAuthority().equals("RIGHT_2") && right.getDescription() == null));
  }

  @Test
  void testCreateRoleWithRights() {
    RoleDto roleDto =
        RoleDto.builder()
            .name("MAP_ROLE")
            .description("MAP_ROLE")
            .rights(
                Set.of(
                    Right.builder().authority("RIGHT_1").build(),
                    Right.builder().authority("RIGHT_2").build()))
            .build();
    Role role = roleDto.toRole();
    assertThat(role.getName()).isEqualTo("MAP_ROLE");
    assertThat(role.getDescription()).isEqualTo("MAP_ROLE");
    assertThat(role.getRights()).hasSize(2);
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right -> right.getAuthority().equals("RIGHT_1") && right.getDescription() == null));
    assertTrue(
        role.getRights().stream()
            .anyMatch(
                right -> right.getAuthority().equals("RIGHT_2") && right.getDescription() == null));
  }
}
