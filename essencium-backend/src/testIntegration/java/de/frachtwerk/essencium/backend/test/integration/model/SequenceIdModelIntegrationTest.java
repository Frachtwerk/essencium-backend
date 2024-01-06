/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class SequenceIdModelIntegrationTest {
  public static final String ADMIN_ROLE_NAME = "ADMIN";

  @Autowired private TestBaseUserRepository repository;
  @Autowired private RoleRepository roleRepository;

  private List<TestUser> savedTestEntities;

  @BeforeEach
  void setUp() {
    savedTestEntities = new LinkedList<>();
  }

  @AfterEach
  void tearDown() {
    repository.deleteAll(savedTestEntities);
  }

  @Test
  void setCreatedAt() {
    final Role adminRole = roleRepository.findByName(ADMIN_ROLE_NAME);
    var testEntity = new TestUser();
    testEntity.setFirstName("Don´t care");
    testEntity.setLastName("Don´t care");
    testEntity.setEmail("i@dont.care");
    testEntity.setRoles(new HashSet<>(Set.of(adminRole)));

    var savedTestEntity = saveEntity(testEntity);
    assertThat(savedTestEntity.getCreatedAt()).isNotNull();
    assertThat(savedTestEntity.getCreatedAt())
        .isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
  }

  @Test
  void setUpdatedAt() {
    final Role adminRole = roleRepository.findByName(ADMIN_ROLE_NAME);
    var testEntity = new TestUser();
    testEntity.setFirstName("Don´t care");
    testEntity.setLastName("Don´t care");
    testEntity.setEmail("i@dont.care");
    testEntity.setRoles(new HashSet<>(Set.of(adminRole)));
    TestUser savedTestEntity = saveEntity(testEntity);
    var initialCreatedAt = savedTestEntity.getCreatedAt();
    savedTestEntity.setEmail("i.really@dont.care");
    TestUser updatedTestEntity = repository.saveAndFlush(savedTestEntity);

    assertThat(updatedTestEntity.getUpdatedAt()).isNotNull();
    assertThat(updatedTestEntity.getUpdatedAt())
        .isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
    assertThat(updatedTestEntity.getCreatedAt()).isEqualTo(initialCreatedAt);
  }

  private TestUser saveEntity(@NotNull TestUser toSave) {
    TestUser saved = repository.save(toSave);
    savedTestEntities.add(saved);

    return saved;
  }
}
