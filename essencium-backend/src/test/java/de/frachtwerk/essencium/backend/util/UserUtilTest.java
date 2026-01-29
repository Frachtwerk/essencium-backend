/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {

  @Mock private SecurityContext securityContext;

  @Mock private Authentication authentication;

  @Mock private EssenciumUserDetails<Long> userDetails;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  class GetUserDetailsFromAuthenticationTests {

    @Test
    void shouldReturnEmptyOptional_whenAuthenticationIsNull() {
      when(securityContext.getAuthentication()).thenReturn(null);

      Optional<EssenciumUserDetails<? extends java.io.Serializable>> result =
          UserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptional_whenPrincipalIsNull() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(null);

      Optional<EssenciumUserDetails<? extends java.io.Serializable>> result =
          UserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptional_whenPrincipalIsNotUserDetails() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn("some-string-principal");

      Optional<EssenciumUserDetails<? extends java.io.Serializable>> result =
          UserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnUserDetails_whenPrincipalIsUserDetails() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(userDetails);

      Optional<EssenciumUserDetails<? extends java.io.Serializable>> result =
          UserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isPresent());
      assertEquals(userDetails, result.get());
    }

    @Test
    void shouldWorkWithDifferentIdTypes() {
      @SuppressWarnings("unchecked")
      EssenciumUserDetails<String> stringIdUserDetails = mock(EssenciumUserDetails.class);
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(stringIdUserDetails);

      Optional<EssenciumUserDetails<? extends java.io.Serializable>> result =
          UserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isPresent());
      assertEquals(stringIdUserDetails, result.get());
    }
  }

  @Nested
  class GetRightsFromUserDetailsTests {

    @Test
    void shouldReturnEmptySet_whenUserDetailsHasNoRights() {
      Set<? extends GrantedAuthority> emptyRights = Collections.emptySet();
      doReturn(emptyRights).when(userDetails).getRights();

      HashSet<String> result = UserUtil.getRightsFromUserDetails(userDetails);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnRights_whenUserDetailsHasRights() {
      Set<? extends GrantedAuthority> rights =
          Set.of(
              new SimpleGrantedAuthority("READ"),
              new SimpleGrantedAuthority("WRITE"),
              new SimpleGrantedAuthority("DELETE"));
      doReturn(rights).when(userDetails).getRights();

      HashSet<String> result = UserUtil.getRightsFromUserDetails(userDetails);

      assertNotNull(result);
      assertEquals(3, result.size());
      assertTrue(result.contains("READ"));
      assertTrue(result.contains("WRITE"));
      assertTrue(result.contains("DELETE"));
    }

    @Test
    void shouldReturnHashSet() {
      Set<? extends GrantedAuthority> rights = Set.of(new SimpleGrantedAuthority("ADMIN"));
      doReturn(rights).when(userDetails).getRights();

      HashSet<String> result = UserUtil.getRightsFromUserDetails(userDetails);

      assertInstanceOf(HashSet.class, result);
    }
  }

  @Nested
  class HasRoleTests {

    @Test
    void shouldReturnFalse_whenUserDetailsIsNull() {
      boolean result = UserUtil.hasRole(null, "ADMIN");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRoleIsNull() {
      boolean result = UserUtil.hasRole(userDetails, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenBothParametersAreNull() {
      boolean result = UserUtil.hasRole(null, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenUserDetailsHasNoRoles() {
      Set<? extends GrantedAuthority> emptyRoles = Collections.emptySet();
      doReturn(emptyRoles).when(userDetails).getRoles();

      boolean result = UserUtil.hasRole(userDetails, "ADMIN");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRoleNotFound() {
      Set<? extends GrantedAuthority> roles =
          Set.of(new SimpleGrantedAuthority("USER"), new SimpleGrantedAuthority("MODERATOR"));
      doReturn(roles).when(userDetails).getRoles();

      boolean result = UserUtil.hasRole(userDetails, "ADMIN");
      assertFalse(result);
    }

    @Test
    void shouldReturnTrue_whenRoleExists() {
      Set<? extends GrantedAuthority> roles =
          Set.of(
              new SimpleGrantedAuthority("USER"),
              new SimpleGrantedAuthority("ADMIN"),
              new SimpleGrantedAuthority("MODERATOR"));
      doReturn(roles).when(userDetails).getRoles();

      boolean result = UserUtil.hasRole(userDetails, "ADMIN");
      assertTrue(result);
    }

    @Test
    void shouldReturnTrue_whenRoleExistsAsOnlyRole() {
      Set<? extends GrantedAuthority> roles = Set.of(new SimpleGrantedAuthority("ADMIN"));
      doReturn(roles).when(userDetails).getRoles();

      boolean result = UserUtil.hasRole(userDetails, "ADMIN");
      assertTrue(result);
    }
  }

  @Nested
  class HasRightTests {

    @Test
    void shouldReturnFalse_whenUserDetailsIsNull() {
      boolean result = UserUtil.hasRight(null, "READ");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRightIsNull() {
      boolean result = UserUtil.hasRight(userDetails, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenBothParametersAreNull() {
      boolean result = UserUtil.hasRight(null, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenUserDetailsHasNoRights() {
      Set<? extends GrantedAuthority> emptyRights = Collections.emptySet();
      doReturn(emptyRights).when(userDetails).getRights();

      boolean result = UserUtil.hasRight(userDetails, "READ");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRightNotFound() {
      Set<? extends GrantedAuthority> rights =
          Set.of(new SimpleGrantedAuthority("WRITE"), new SimpleGrantedAuthority("DELETE"));
      doReturn(rights).when(userDetails).getRights();

      boolean result = UserUtil.hasRight(userDetails, "READ");
      assertFalse(result);
    }

    @Test
    void shouldReturnTrue_whenRightExists() {
      Set<? extends GrantedAuthority> rights =
          Set.of(
              new SimpleGrantedAuthority("READ"),
              new SimpleGrantedAuthority("WRITE"),
              new SimpleGrantedAuthority("DELETE"));
      doReturn(rights).when(userDetails).getRights();

      boolean result = UserUtil.hasRight(userDetails, "READ");
      assertTrue(result);
    }

    @Test
    void shouldReturnTrue_whenRightExistsAsOnlyRight() {
      Set<? extends GrantedAuthority> rights = Set.of(new SimpleGrantedAuthority("READ"));
      doReturn(rights).when(userDetails).getRights();

      boolean result = UserUtil.hasRight(userDetails, "READ");
      assertTrue(result);
    }
  }
}
