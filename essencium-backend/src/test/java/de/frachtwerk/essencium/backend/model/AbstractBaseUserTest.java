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
import static org.mockito.Mockito.*;

import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractBaseUserTest {

  private TestUser testUser;

  // Konkrete Implementierung von AbstractBaseUser für Tests
  @Getter
  @Setter
  @SuperBuilder(toBuilder = true)
  @NoArgsConstructor
  static class TestUser extends AbstractBaseUser<Long> {
    private Long id;

    public TestUser(Long id, String email, String firstName, String lastName) {
      this.setId(id);
      this.setEmail(email);
      this.setFirstName(firstName);
      this.setLastName(lastName);
    }

    @Override
    public String getTitle() {
      return toString();
    }
  }

  @BeforeEach
  void setUp() {
    testUser = new TestUser(1L, "test@example.com", "Max", "Mustermann");
  }

  @Nested
  class EqualsTest {

    @Test
    void testEquals_SameObject_ShouldReturnTrue() {
      assertEquals(testUser, testUser);
    }

    @Test
    void testEquals_NullObject_ShouldReturnFalse() {
      assertNotEquals(null, testUser);
    }

    @Test
    void testEquals_SameIdAndEmail_ShouldReturnTrue() {
      TestUser sameUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(testUser, sameUser);
    }

    @Test
    void testEquals_DifferentId_ShouldReturnFalse() {
      TestUser differentUser = new TestUser(99L, "test@example.com", "Max", "Mustermann");

      assertNotEquals(testUser, differentUser);
    }

    @Test
    void testEquals_DifferentEmail_ShouldReturnFalse() {
      TestUser differentUser = new TestUser(1L, "different@example.com", "Max", "Mustermann");

      assertNotEquals(testUser, differentUser);
    }

    @Test
    void testEquals_DifferentClass_ShouldReturnFalse() {
      String differentObject = "not a user";

      assertNotEquals(differentObject, testUser);
    }

    @Test
    void testEquals_WithHibernateProxy_ShouldHandleProxyCorrectly() {
      // Create a real test user that we will use as a “proxy target.”
      TestUser proxyTarget = new TestUser(1L, "test@example.com", "Max", "Mustermann");

      // Create a mock that implements both HibernateProxy and TestUser.
      TestUser hibernateProxy =
          mock(TestUser.class, withSettings().extraInterfaces(HibernateProxy.class));
      LazyInitializer lazyInitializer = mock(LazyInitializer.class);
      when(((HibernateProxy) hibernateProxy).getHibernateLazyInitializer())
          .thenReturn(lazyInitializer);
      when(lazyInitializer.getPersistentClass()).thenAnswer(invocation -> TestUser.class);

      // Configure the proxy's TestUser methods
      when(hibernateProxy.getId()).thenReturn(proxyTarget.getId());
      when(hibernateProxy.getEmail()).thenReturn(proxyTarget.getEmail());

      TestUser sameDataUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(sameDataUser, hibernateProxy);
    }

    @Test
    void testEquals_EqualsContract_ShouldBeReflexive() {
      // Reflexive: x.equals(x) should be true
      assertEquals(testUser, testUser);
    }

    @Test
    void testEquals_EqualsContract_ShouldBeSymmetric() {
      // Symmetrical: x.equals(y) == y.equals(x)
      TestUser sameUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(testUser, sameUser);
      assertEquals(sameUser, testUser);
    }

    @Test
    void testEquals_EqualsContract_ShouldBeTransitive() {
      // Transitive: if x equals y and y equals z, then x equals z.
      TestUser user1 = new TestUser(1L, "test@example.com", "Max", "Mustermann");
      TestUser user2 = new TestUser(1L, "test@example.com", "Anna", "Schmidt");
      TestUser user3 = new TestUser(1L, "test@example.com", "John", "Doe");

      assertEquals(user1, user2);
      assertEquals(user2, user3);
      assertEquals(user1, user3);
    }

    @Test
    void testEquals_EqualsContract_ShouldBeConsistent() {
      // Consistent: multiple calls should produce the same result
      TestUser sameUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(testUser, sameUser);
      assertEquals(testUser, sameUser); // second Invocation
      assertEquals(testUser, sameUser); // third Invocation
    }
  }

  @Nested
  class HashCodeTest {
    @Test
    void testHashCode_SameIdAndEmail_ShouldReturnSameHashCode() {
      TestUser sameUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(testUser.hashCode(), sameUser.hashCode());
    }

    @Test
    void testHashCode_DifferentIdOrEmail_ShouldReturnDifferentHashCode() {
      TestUser differentUser = new TestUser(99L, "different@example.com", "Max", "Mustermann");

      assertNotEquals(testUser.hashCode(), differentUser.hashCode());
    }

    @Test
    void testHashCode_NullId_ShouldHandleGracefully() {
      TestUser userWithNullId = new TestUser(null, "test@example.com", "Max", "Mustermann");

      assertDoesNotThrow(userWithNullId::hashCode);
    }

    @Test
    void testHashCode_NullEmail_ShouldHandleGracefully() {
      TestUser userWithNullEmail = new TestUser(1L, null, "Max", "Mustermann");

      assertDoesNotThrow(userWithNullEmail::hashCode);
    }

    @Test
    void testHashCode_HashCodeContract_ConsistentWithEquals() {
      // Wenn zwei Objekte gleich sind, müssen sie denselben hashCode haben
      TestUser sameUser = new TestUser(1L, "test@example.com", "Different", "Name");

      assertEquals(testUser, sameUser);
      assertEquals(testUser.hashCode(), sameUser.hashCode());
    }

    @Test
    void testHashCode_HashCodeContract_ShouldBeConsistent() {
      // Mehrfache Aufrufe von hashCode() sollten denselben Wert zurückgeben
      int firstCall = testUser.hashCode();
      int secondCall = testUser.hashCode();
      int thirdCall = testUser.hashCode();

      assertEquals(firstCall, secondCall);
      assertEquals(secondCall, thirdCall);
    }
  }

  @Nested
  class ToStringTest {
    @Test
    void testToString_ShouldContainIdAndEmail() {
      String result = testUser.toString();

      assertTrue(result.contains("1"), "toString sollte die ID enthalten");
      assertTrue(result.contains("test@example.com"), "toString sollte die Email enthalten");
      assertTrue(result.startsWith("User"), "toString sollte mit 'User' beginnen");
    }

    @Test
    void testToString_WithNullId_ShouldHandleGracefully() {
      TestUser userWithNullId = new TestUser(null, "test@example.com", "Max", "Mustermann");

      String result = userWithNullId.toString();

      assertNotNull(result);
      assertTrue(result.contains("test@example.com"));
      assertTrue(result.startsWith("User"));
    }

    @Test
    void testToString_WithNullEmail_ShouldHandleGracefully() {
      TestUser userWithNullEmail = new TestUser(1L, null, "Max", "Mustermann");

      String result = userWithNullEmail.toString();

      assertNotNull(result);
      assertTrue(result.contains("1"));
      assertTrue(result.startsWith("User"));
    }
  }
}
