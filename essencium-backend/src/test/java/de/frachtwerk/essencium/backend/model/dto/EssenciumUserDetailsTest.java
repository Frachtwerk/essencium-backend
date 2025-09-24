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

package de.frachtwerk.essencium.backend.model.dto;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class EssenciumUserDetailsTest {
  // Hilfsmethode für Authority-Mocks
  private GrantedAuthority mockAuthority(String name) {
    GrantedAuthority auth = Mockito.mock(GrantedAuthority.class);
    when(auth.getAuthority()).thenReturn(name);
    return auth;
  }

  @Test
  void testGetId_Long() {
    EssenciumUserDetails<Long> user =
        EssenciumUserDetails.<Long>builder()
            .id(42L)
            .username("testuser")
            .firstName("Max")
            .lastName("Mustermann")
            .locale("de")
            .roles(Set.of(mockAuthority("ROLE_USER")))
            .rights(Set.of(mockAuthority("RIGHT_READ")))
            .additionalClaims(Map.of("foo", "bar"))
            .build();
    assertEquals(42L, user.getId());
    assertTrue(
        user.getRoles().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
            .contains("ROLE_USER"));
    assertTrue(
        user.getRights().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
            .contains("RIGHT_READ"));
    assertThat(user.getAdditionalClaims()).containsEntry("foo", "bar");
  }

  @Test
  void testGetLocale_Valid() {
    EssenciumUserDetails<Long> user =
        EssenciumUserDetails.<Long>builder()
            .id(1L)
            .username("test")
            .firstName("")
            .lastName("")
            .locale("de")
            .roles(Set.of())
            .rights(Set.of())
            .additionalClaims(Map.of())
            .build();
    assertEquals(Locale.GERMAN, user.getLocale());
  }

  @Test
  void testGetLocale_Fallback() {
    EssenciumUserDetails<Long> user =
        EssenciumUserDetails.<Long>builder()
            .id(1L)
            .username("")
            .firstName("")
            .lastName("")
            .locale("")
            .roles(Set.of())
            .rights(Set.of())
            .additionalClaims(Map.of())
            .build();
    assertEquals(AbstractBaseUser.DEFAULT_LOCALE, user.getLocale());
  }

  @Test
  void testGetAdditionalClaimByKey_Typen() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("long", 123L);
    claims.put("int", 42);
    claims.put("str", "99");
    claims.put("bool", true);
    claims.put("double", 123.456);
    claims.put("null", null);
    EssenciumUserDetails<Long> user =
        EssenciumUserDetails.<Long>builder()
            .id(1L)
            .username("")
            .firstName("")
            .lastName("")
            .locale("")
            .roles(Set.of())
            .rights(Set.of())
            .additionalClaims(claims)
            .build();
    assertEquals(123L, user.getAdditionalClaimByKey("long", Long.class));
    assertEquals(42, user.getAdditionalClaimByKey("int", Integer.class));
    assertEquals("99", user.getAdditionalClaimByKey("str", String.class));
    assertEquals(true, user.getAdditionalClaimByKey("bool", Boolean.class));
    assertEquals(123.456, user.getAdditionalClaimByKey("double", Double.class));
    assertNull(user.getAdditionalClaimByKey("null", Object.class));
    assertNull(user.getAdditionalClaimByKey("nonexistent", Object.class));
    assertNull(user.getAdditionalClaimByKey("null"));
    assertNull(user.getAdditionalClaimByKey("nonexistent"));
  }

  @Test
  void testGetAuthorities() {
    GrantedAuthority role = mockAuthority("ROLE_USER");
    GrantedAuthority right = mockAuthority("RIGHT_READ");
    EssenciumUserDetails<Long> user =
        EssenciumUserDetails.<Long>builder()
            .id(1L)
            .username("")
            .firstName("")
            .lastName("")
            .locale("")
            .roles(Set.of(role))
            .rights(Set.of(right))
            .additionalClaims(Map.of())
            .build();
    Set<String> authorities = new HashSet<>();
    user.getAuthorities().forEach(a -> authorities.add(a.getAuthority()));
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("RIGHT_READ"));
  }

  @Test
  void testGetId_UUID() {
    UUID uuid = UUID.randomUUID();
    EssenciumUserDetails<UUID> user =
        EssenciumUserDetails.<UUID>builder()
            .id(uuid)
            .username("testuser")
            .firstName("Max")
            .lastName("Mustermann")
            .locale("de")
            .roles(Set.of(mockAuthority("ROLE_USER")))
            .rights(Set.of(mockAuthority("RIGHT_READ")))
            .additionalClaims(Map.of("foo", "bar"))
            .build();
    assertEquals(uuid, user.getId());
    assertTrue(
        user.getRoles().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
            .contains("ROLE_USER"));
    assertTrue(
        user.getRights().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
            .contains("RIGHT_READ"));
  }
}
