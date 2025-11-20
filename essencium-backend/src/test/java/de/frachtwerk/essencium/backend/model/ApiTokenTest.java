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

package de.frachtwerk.essencium.backend.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiTokenTest {

  private ApiToken apiToken;
  private static final String TEST_USER = "test@example.com";
  private static final String TEST_DESCRIPTION = "Test Token";

  @BeforeEach
  void setUp() {
    apiToken =
        ApiToken.builder()
            .linkedUser(TEST_USER)
            .description(TEST_DESCRIPTION)
            .status(ApiTokenStatus.ACTIVE)
            .rights(new HashSet<>())
            .build();
  }

  @Test
  void testBuilder() {
    LocalDate validUntil = LocalDate.now().plusDays(30);
    Set<Right> rights = new HashSet<>();
    Right testRight = new Right();
    testRight.setAuthority("TEST_RIGHT");
    rights.add(testRight);

    ApiToken token =
        ApiToken.builder()
            .linkedUser(TEST_USER)
            .description(TEST_DESCRIPTION)
            .validUntil(validUntil)
            .status(ApiTokenStatus.ACTIVE)
            .rights(rights)
            .token("sample-token")
            .build();

    assertNotNull(token);
    assertEquals(TEST_USER, token.getLinkedUser());
    assertEquals(TEST_DESCRIPTION, token.getDescription());
    assertEquals(validUntil, token.getValidUntil());
    assertEquals(ApiTokenStatus.ACTIVE, token.getStatus());
    assertEquals(rights, token.getRights());
    assertEquals("sample-token", token.getToken());
  }

  @Test
  void testGetAuthorities() {
    Set<Right> rights = new HashSet<>();
    Right right1 = new Right();
    right1.setAuthority("RIGHT_1");
    Right right2 = new Right();
    right2.setAuthority("RIGHT_2");
    rights.add(right1);
    rights.add(right2);
    apiToken.setRights(rights);

    var authorities = apiToken.getAuthorities();

    assertNotNull(authorities);
    assertEquals(2, authorities.size());
    assertTrue(authorities.contains(right1));
    assertTrue(authorities.contains(right2));
  }

  @Test
  void testGetPassword() {
    String password = apiToken.getPassword();

    assertNull(password);
  }

  @Test
  void testGetUsername() {
    UUID id = UUID.randomUUID();
    apiToken.setId(id);

    String username = apiToken.getUsername();

    assertEquals(TEST_USER + "-api-token-" + id, username);
  }

  @Test
  void testIsAccountNonExpired_WithActiveStatusAndNoExpiration() {
    boolean result = apiToken.isAccountNonExpired();

    assertTrue(result);
  }

  @Test
  void testIsAccountNonExpired_WithActiveStatusAndFutureExpiration() {
    apiToken.setValidUntil(LocalDate.now().plusDays(10));

    boolean result = apiToken.isAccountNonExpired();

    assertTrue(result);
  }

  @Test
  void testIsAccountNonExpired_WithPastExpiration() {
    apiToken.setValidUntil(LocalDate.now().minusDays(1));

    boolean result = apiToken.isAccountNonExpired();

    assertFalse(result);
  }

  @Test
  void testIsAccountNonExpired_WithRevokedStatus() {
    apiToken.setStatus(ApiTokenStatus.REVOKED);

    boolean result = apiToken.isAccountNonExpired();

    assertFalse(result);
  }

  @Test
  void testIsAccountNonExpired_WithTodayExpiration() {
    apiToken.setValidUntil(LocalDate.now());

    boolean result = apiToken.isAccountNonExpired();

    assertTrue(result);
  }

  @Test
  void testIsAccountNonLocked() {
    apiToken.setValidUntil(LocalDate.now().plusDays(10));

    boolean result = apiToken.isAccountNonLocked();

    assertTrue(result);
    assertEquals(apiToken.isAccountNonExpired(), result);
  }

  @Test
  void testIsCredentialsNonExpired() {
    apiToken.setValidUntil(LocalDate.now().plusDays(10));

    boolean result = apiToken.isCredentialsNonExpired();

    assertTrue(result);
    assertEquals(apiToken.isAccountNonExpired(), result);
  }

  @Test
  void testIsEnabled() {
    apiToken.setValidUntil(LocalDate.now().plusDays(10));

    boolean result = apiToken.isEnabled();

    assertTrue(result);
    assertEquals(apiToken.isAccountNonExpired(), result);
  }

  @Test
  void testIsEnabled_WithExpiredToken() {
    apiToken.setValidUntil(LocalDate.now().minusDays(1));

    boolean result = apiToken.isEnabled();

    assertFalse(result);
  }

  @Test
  void testGetTitle() {
    UUID id = UUID.randomUUID();
    apiToken.setId(id);

    String title = apiToken.getTitle();

    assertEquals(apiToken.getUsername(), title);
    assertEquals(TEST_USER + "-api-token-" + id, title);
  }

  @Test
  void testEquals_SameObject() {
    assertEquals(apiToken, apiToken);
  }

  @Test
  void testEquals_EqualObjects() {
    UUID id = UUID.randomUUID();
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(id);

    ApiToken token2 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token2.setId(id);

    assertEquals(token1, token2);
  }

  @Test
  void testEquals_DifferentId() {
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(UUID.randomUUID());

    ApiToken token2 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token2.setId(UUID.randomUUID());

    assertNotEquals(token1, token2);
  }

  @Test
  void testEquals_DifferentLinkedUser() {
    UUID id = UUID.randomUUID();
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(id);

    ApiToken token2 =
        ApiToken.builder()
            .linkedUser("different@example.com")
            .description(TEST_DESCRIPTION)
            .build();
    token2.setId(id);

    assertNotEquals(token1, token2);
  }

  @Test
  void testEquals_DifferentDescription() {
    UUID id = UUID.randomUUID();
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(id);

    ApiToken token2 =
        ApiToken.builder().linkedUser(TEST_USER).description("Different Description").build();
    token2.setId(id);

    assertNotEquals(token1, token2);
  }

  @Test
  void testEquals_NullObject() {
    assertNotEquals(null, apiToken);
  }

  @Test
  void testEquals_DifferentClass() {
    assertNotEquals(new Object(), apiToken);
  }

  @Test
  void testHashCode_EqualObjects() {
    UUID id = UUID.randomUUID();
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(id);

    ApiToken token2 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token2.setId(id);

    assertEquals(token1.hashCode(), token2.hashCode());
  }

  @Test
  void testHashCode_DifferentObjects() {
    ApiToken token1 =
        ApiToken.builder().linkedUser(TEST_USER).description(TEST_DESCRIPTION).build();
    token1.setId(UUID.randomUUID());

    ApiToken token2 =
        ApiToken.builder()
            .linkedUser("different@example.com")
            .description("Different Description")
            .build();
    token2.setId(UUID.randomUUID());

    assertNotEquals(token1.hashCode(), token2.hashCode());
  }

  @Test
  void testToBuilder() {
    UUID id = UUID.randomUUID();
    LocalDate validUntil = LocalDate.now().plusDays(30);
    apiToken.setId(id);
    apiToken.setValidUntil(validUntil);

    ApiToken clonedToken = apiToken.toBuilder().build();

    assertEquals(apiToken.getId(), clonedToken.getId());
    assertEquals(apiToken.getLinkedUser(), clonedToken.getLinkedUser());
    assertEquals(apiToken.getDescription(), clonedToken.getDescription());
    assertEquals(apiToken.getValidUntil(), clonedToken.getValidUntil());
    assertEquals(apiToken.getStatus(), clonedToken.getStatus());
  }

  @Test
  void testDefaultStatus() {
    ApiToken token = ApiToken.builder().linkedUser(TEST_USER).build();

    assertEquals(ApiTokenStatus.ACTIVE, token.getStatus());
  }

  @Test
  void testDefaultRights() {
    ApiToken token = ApiToken.builder().linkedUser(TEST_USER).build();

    assertNotNull(token.getRights());
    assertTrue(token.getRights().isEmpty());
  }
}
