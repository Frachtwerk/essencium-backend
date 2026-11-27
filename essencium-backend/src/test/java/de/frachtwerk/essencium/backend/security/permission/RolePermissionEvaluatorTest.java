/*
 * Copyright (C) 2026-7613 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RolePermissionEvaluatorTest {

  private final RoleService roleService = mock(RoleService.class);
  private final RolePermissionEvaluator evaluator = new RolePermissionEvaluator(roleService);

  private static Authentication withRights(BasicApplicationRight... rights) {
    List<SimpleGrantedAuthority> authorities =
        Arrays.stream(rights).map(r -> new SimpleGrantedAuthority(r.getAuthority())).toList();
    return new UsernamePasswordAuthenticationToken("user", null, authorities);
  }

  @Test
  void supportsOnlyRole() {
    assertThat(evaluator.supports(Role.class)).isTrue();
    assertThat(evaluator.supports("Role")).isTrue();
    assertThat(evaluator.supports(Object.class)).isFalse();
    assertThat(evaluator.supports("User")).isFalse();
  }

  @Test
  void grantsPermissionWithMatchingRight() {
    Role role = Role.builder().name("USER").build();
    Authentication auth =
        withRights(
            BasicApplicationRight.ROLE_CREATE,
            BasicApplicationRight.ROLE_READ,
            BasicApplicationRight.ROLE_UPDATE,
            BasicApplicationRight.ROLE_DELETE);

    assertThat(evaluator.hasPermission(auth, role, "create")).isTrue();
    assertThat(evaluator.hasPermission(auth, role, "read")).isTrue();
    assertThat(evaluator.hasPermission(auth, role, "update")).isTrue();
    assertThat(evaluator.hasPermission(auth, role, "delete")).isTrue();
    assertThat(evaluator.hasPermission(auth, role, "unknown")).isFalse();
  }

  @Test
  void deniesPermissionWithoutMatchingRight() {
    Role role = Role.builder().name("USER").build();
    Authentication auth = withRights(BasicApplicationRight.ROLE_READ);

    assertThat(evaluator.hasPermission(auth, role, "update")).isFalse();
    assertThat(evaluator.hasPermission(auth, role, "delete")).isFalse();
  }

  @Test
  void deniesUpdateAndDeleteOfProtectedRole() {
    Role role = Role.builder().name("ADMIN").isProtected(true).build();
    Authentication auth =
        withRights(
            BasicApplicationRight.ROLE_READ,
            BasicApplicationRight.ROLE_UPDATE,
            BasicApplicationRight.ROLE_DELETE);

    assertThat(evaluator.hasPermission(auth, role, "read")).isTrue();
    assertThat(evaluator.hasPermission(auth, role, "update")).isFalse();
    assertThat(evaluator.hasPermission(auth, role, "delete")).isFalse();
  }

  @Test
  void deniesForeignTargets() {
    Authentication auth = withRights(BasicApplicationRight.ROLE_READ);

    assertThat(evaluator.hasPermission(auth, new Object(), "read")).isFalse();
    assertThat(evaluator.hasPermission(auth, "USER", "User", "read")).isFalse();
  }

  @Test
  void loadsRoleByNameForIdBasedCheck() {
    Role role = Role.builder().name("USER").build();
    when(roleService.getByName("USER")).thenReturn(role);

    assertThat(
            evaluator.hasPermission(
                withRights(BasicApplicationRight.ROLE_READ), "USER", "Role", "read"))
        .isTrue();
  }

  @Test
  void deniesIdBasedCheckForUnknownRole() {
    when(roleService.getByName("MISSING")).thenReturn(null);

    assertThat(
            evaluator.hasPermission(
                withRights(BasicApplicationRight.ROLE_READ), "MISSING", "Role", "read"))
        .isFalse();
  }
}
