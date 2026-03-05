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
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import java.io.Serializable;
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
class EssenciumUserUtilTest {

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

      Optional<EssenciumUserDetails<Serializable>> result =
          EssenciumUserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptional_whenPrincipalIsNull() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(null);

      Optional<EssenciumUserDetails<Serializable>> result =
          EssenciumUserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptional_whenPrincipalIsNotEssenciumUserDetails() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn("some-string-principal");

      Optional<EssenciumUserDetails<Serializable>> result =
          EssenciumUserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnUserDetails_whenPrincipalIsEssenciumUserDetails() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(userDetails);

      Optional<EssenciumUserDetails<Serializable>> result =
          EssenciumUserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isPresent());
      assertEquals(userDetails, result.get());
    }

    @Test
    void shouldWorkWithDifferentIdTypes() {
      @SuppressWarnings("unchecked")
      EssenciumUserDetails<String> stringIdUserDetails = mock(EssenciumUserDetails.class);
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(stringIdUserDetails);

      Optional<EssenciumUserDetails<Serializable>> result =
          EssenciumUserUtil.getUserDetailsFromAuthentication();
      assertTrue(result.isPresent());
      assertEquals(stringIdUserDetails, result.get());
    }

    @Test
    void shouldThrowException_whenUserDetailsNotPresent() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(null);

      assertThrows(
          NotAllowedException.class,
          () ->
              EssenciumUserUtil.getUserDetailsFromAuthenticationOrThrow(
                  "No authenticated user found in security context"));
    }
  }

  @Nested
  class GetRightsFromUserDetailsTests {

    @Test
    void shouldReturnEmptySet_whenUserDetailsHasNoRights() {
      Set<? extends GrantedAuthority> emptyRights = Collections.emptySet();
      doReturn(emptyRights).when(userDetails).getRights();

      HashSet<String> result = EssenciumUserUtil.getRightsFromUserDetails(userDetails);

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

      HashSet<String> result = EssenciumUserUtil.getRightsFromUserDetails(userDetails);

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

      HashSet<String> result = EssenciumUserUtil.getRightsFromUserDetails(userDetails);

      assertInstanceOf(HashSet.class, result);
    }
  }

  @Nested
  class HasRoleTests {

    @Test
    void shouldReturnFalse_whenUserDetailsIsNull() {
      boolean result = EssenciumUserUtil.hasRole(null, "ADMIN");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRoleIsNull() {
      boolean result = EssenciumUserUtil.hasRole(userDetails, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenBothParametersAreNull() {
      boolean result = EssenciumUserUtil.hasRole(null, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenUserDetailsHasNoRoles() {
      Set<? extends GrantedAuthority> emptyRoles = Collections.emptySet();
      doReturn(emptyRoles).when(userDetails).getRoles();

      boolean result = EssenciumUserUtil.hasRole(userDetails, "ADMIN");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRoleNotFound() {
      Set<? extends GrantedAuthority> roles =
          Set.of(new SimpleGrantedAuthority("USER"), new SimpleGrantedAuthority("MODERATOR"));
      doReturn(roles).when(userDetails).getRoles();

      boolean result = EssenciumUserUtil.hasRole(userDetails, "ADMIN");
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

      boolean result = EssenciumUserUtil.hasRole(userDetails, "ADMIN");
      assertTrue(result);
    }

    @Test
    void shouldReturnTrue_whenRoleExistsAsOnlyRole() {
      Set<? extends GrantedAuthority> roles = Set.of(new SimpleGrantedAuthority("ADMIN"));
      doReturn(roles).when(userDetails).getRoles();

      boolean result = EssenciumUserUtil.hasRole(userDetails, "ADMIN");
      assertTrue(result);
    }
  }

  @Nested
  class HasOneOfRoles {
    @Test
    void returnsFalse_whenUserDetailsIsNull() {
      assertFalse(EssenciumUserUtil.hasOneOfRoles(null, "ANY_ROLE"));
    }

    @Test
    void returnsFalse_whenRolesIsNull() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      assertFalse(EssenciumUserUtil.hasOneOfRoles(userDetails, (String[]) null));
    }

    @Test
    void returnsTrue_whenUserHasMatchingRole() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      GrantedAuthority authority = mock(GrantedAuthority.class);
      when(authority.getAuthority()).thenReturn("ROLE_1");
      when(userDetails.getRoles()).thenAnswer(i -> Set.of(authority));
      assertTrue(EssenciumUserUtil.hasOneOfRoles(userDetails, "ROLE_1"));
    }

    @Test
    void returnsFalse_whenUserHasNoMatchingRole() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      GrantedAuthority authority = mock(GrantedAuthority.class);
      when(authority.getAuthority()).thenReturn("ROLE_1");
      when(userDetails.getRoles()).thenAnswer(i -> Set.of(authority));
      assertFalse(EssenciumUserUtil.hasOneOfRoles(userDetails, "ROLE_2"));
    }

    @Test
    void returnsTrue_whenUserHasOneOfMultipleRoles() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      GrantedAuthority authority1 = mock(GrantedAuthority.class);
      when(authority1.getAuthority()).thenReturn("ROLE_1");
      GrantedAuthority authority2 = mock(GrantedAuthority.class);
      when(authority2.getAuthority()).thenReturn("ROLE_2");
      when(userDetails.getRoles()).thenAnswer(i -> Set.of(authority1, authority2));
      assertTrue(EssenciumUserUtil.hasOneOfRoles(userDetails, "ROLE_1", "ROLE_3"));
    }

    @Test
    void returnsFalse_whenUserHasNoRoles() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      when(userDetails.getRoles()).thenReturn(Set.of());
      assertFalse(EssenciumUserUtil.hasOneOfRoles(userDetails, "ANY_ROLE"));
    }
  }

  @Nested
  class HasAllRoles {
    @Test
    void returnsFalse_whenUserDetailsIsNull() {
      assertFalse(EssenciumUserUtil.hasAllRoles(null, "ANY_ROLE"));
    }

    @Test
    void returnsFalse_whenRolesIsNull() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      assertFalse(EssenciumUserUtil.hasAllRoles(userDetails, (String[]) null));
    }

    @Test
    void returnsTrue_whenUserHasAllRoles() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      GrantedAuthority authority1 = mock(GrantedAuthority.class);
      when(authority1.getAuthority()).thenReturn("ROLE_1");
      GrantedAuthority authority2 = mock(GrantedAuthority.class);
      when(authority2.getAuthority()).thenReturn("ROLE_2");
      when(userDetails.getRoles()).thenAnswer(i -> Set.of(authority1, authority2));
      assertTrue(EssenciumUserUtil.hasAllRoles(userDetails, "ROLE_1", "ROLE_2"));
    }

    @Test
    void returnsFalse_whenUserMissingOneRole() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      GrantedAuthority authority1 = mock(GrantedAuthority.class);
      when(authority1.getAuthority()).thenReturn("ROLE_1");
      when(userDetails.getRoles()).thenAnswer(i -> Set.of(authority1));
      assertFalse(EssenciumUserUtil.hasAllRoles(userDetails, "ROLE_1", "ROLE_2"));
    }

    @Test
    void returnsFalse_whenUserHasNoRoles() {
      EssenciumUserDetails<?> userDetails = mock(EssenciumUserDetails.class);
      when(userDetails.getRoles()).thenReturn(Set.of());
      assertFalse(EssenciumUserUtil.hasAllRoles(userDetails, "ANY_ROLE"));
    }
  }

  @Nested
  class HasRightTests {

    @Test
    void shouldReturnFalse_whenUserDetailsIsNull() {
      boolean result = EssenciumUserUtil.hasRight(null, "READ");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRightIsNull() {
      boolean result = EssenciumUserUtil.hasRight(userDetails, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenBothParametersAreNull() {
      boolean result = EssenciumUserUtil.hasRight(null, null);
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenUserDetailsHasNoRights() {
      Set<? extends GrantedAuthority> emptyRights = Collections.emptySet();
      doReturn(emptyRights).when(userDetails).getRights();

      boolean result = EssenciumUserUtil.hasRight(userDetails, "READ");
      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenRightNotFound() {
      Set<? extends GrantedAuthority> rights =
          Set.of(new SimpleGrantedAuthority("WRITE"), new SimpleGrantedAuthority("DELETE"));
      doReturn(rights).when(userDetails).getRights();

      boolean result = EssenciumUserUtil.hasRight(userDetails, "READ");
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

      boolean result = EssenciumUserUtil.hasRight(userDetails, "READ");
      assertTrue(result);
    }

    @Test
    void shouldReturnTrue_whenRightExistsAsOnlyRight() {
      Set<? extends GrantedAuthority> rights = Set.of(new SimpleGrantedAuthority("READ"));
      doReturn(rights).when(userDetails).getRights();

      boolean result = EssenciumUserUtil.hasRight(userDetails, "READ");
      assertTrue(result);
    }
  }

  @Nested
  class GetUserLocale {
    @AfterEach
    void tearDown() {
      SecurityContextHolder.clearContext();
    }

    @Test
    void returnsDefaultLocale_whenNoUserInAuthenticationContext() {
      when(securityContext.getAuthentication()).thenReturn(null);
      Locale defaultLocale = Locale.getDefault();
      assertEquals(defaultLocale, EssenciumUserUtil.getUserLocale());
    }

    @Test
    void returnsUserLocale_whenUserDetailsPresent() {
      EssenciumUserDetails<?> mockUserDetails = mock(EssenciumUserDetails.class);
      Locale userLocale = Locale.FRENCH;
      when(mockUserDetails.getLocale()).thenReturn(userLocale);

      Authentication mockAuthentication = mock(Authentication.class);
      when(mockAuthentication.getPrincipal()).thenReturn(mockUserDetails);
      when(securityContext.getAuthentication()).thenReturn(mockAuthentication);

      assertEquals(userLocale, EssenciumUserUtil.getUserLocale());
    }
  }
}
