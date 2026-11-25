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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

class DelegatingPermissionEvaluatorTest {

  private final Authentication authentication = mock(Authentication.class);

  @Test
  void isPrimaryBean() {
    assertThat(DelegatingPermissionEvaluator.class).hasAnnotation(Primary.class);
    assertThat(DelegatingPermissionEvaluator.class).hasAnnotation(Component.class);
  }

  @Test
  void noopIsNoBean() {
    assertThat(NoopPermissionEvaluator.class.isAnnotationPresent(Component.class)).isFalse();
  }

  @Test
  void delegatesToSupportingEvaluatorByObject() {
    EntityPermissionEvaluator roleEvaluator = mock(EntityPermissionEvaluator.class);
    Role role = Role.builder().name("ADMIN").build();
    when(roleEvaluator.supports(Role.class)).thenReturn(true);
    when(roleEvaluator.hasPermission(authentication, role, "read")).thenReturn(true);

    DelegatingPermissionEvaluator evaluator =
        new DelegatingPermissionEvaluator(List.of(roleEvaluator));

    assertThat(evaluator.hasPermission(authentication, role, "read")).isTrue();
    verify(roleEvaluator).hasPermission(authentication, role, "read");
  }

  @Test
  void delegatesToSupportingEvaluatorByIdAndType() {
    EntityPermissionEvaluator roleEvaluator = mock(EntityPermissionEvaluator.class);
    when(roleEvaluator.supports("Role")).thenReturn(true);
    when(roleEvaluator.hasPermission(authentication, "ADMIN", "Role", "read")).thenReturn(true);

    DelegatingPermissionEvaluator evaluator =
        new DelegatingPermissionEvaluator(List.of(roleEvaluator));

    assertThat(evaluator.hasPermission(authentication, "ADMIN", "Role", "read")).isTrue();
  }

  @Test
  void deniesWhenNoEvaluatorSupportsTarget() {
    EntityPermissionEvaluator roleEvaluator = mock(EntityPermissionEvaluator.class);
    when(roleEvaluator.supports(any(Class.class))).thenReturn(false);
    when(roleEvaluator.supports(any(String.class))).thenReturn(false);

    DelegatingPermissionEvaluator evaluator =
        new DelegatingPermissionEvaluator(List.of(roleEvaluator));

    assertThat(evaluator.hasPermission(authentication, new Object(), "read")).isFalse();
    assertThat(evaluator.hasPermission(authentication, 1L, "Unknown", "read")).isFalse();
  }

  @Test
  void deniesNullTargetWithoutException() {
    EntityPermissionEvaluator roleEvaluator = mock(EntityPermissionEvaluator.class);

    DelegatingPermissionEvaluator evaluator =
        new DelegatingPermissionEvaluator(List.of(roleEvaluator));

    assertThat(evaluator.hasPermission(authentication, null, "read")).isFalse();
    verifyNoInteractions(roleEvaluator);
  }
}
