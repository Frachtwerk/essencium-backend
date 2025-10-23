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

package de.frachtwerk.essencium.backend.test.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Table(name = "test_user")
public class TestUser extends AbstractBaseUser<Long> {
  public static final String CLAIM_TEST_INTEGER = "testInteger";
  public static final String CLAIM_TEST_LONG = "testLong";
  public static final String CLAIM_TEST_STRING = "testString";
  public static final String CLAIM_TEST_BOOLEAN = "testBoolean";
  public static final String CLAIM_TEST_DOUBLE = "testDouble";
  public static final String CLAIM_TEST_MAP = "testMap";
  public static final String CLAIM_TEST_NON_EXISTENT = "testNonExistent";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Override
  public String getTitle() {
    return getFirstName() + " " + getLastName();
  }

  @Override
  @JsonIgnore
  public Map<String, Object> getAdditionalClaims() {
    HashMap<String, Object> map = new HashMap<>(super.getAdditionalClaims());
    map.put(CLAIM_TEST_INTEGER, 1);
    map.put(CLAIM_TEST_LONG, 2L);
    map.put(CLAIM_TEST_STRING, "test");
    map.put(CLAIM_TEST_BOOLEAN, Boolean.TRUE);
    map.put(CLAIM_TEST_DOUBLE, 3.1);
    map.put(CLAIM_TEST_MAP, Map.of("key1", "value1", "key2", "value2"));
    return map;
  }
}
