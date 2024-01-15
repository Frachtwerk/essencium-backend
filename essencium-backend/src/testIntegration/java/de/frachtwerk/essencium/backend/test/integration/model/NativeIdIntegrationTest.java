/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.app.Native;
import de.frachtwerk.essencium.backend.test.integration.app.NativeDTO;
import de.frachtwerk.essencium.backend.test.integration.app.NativeService;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NativeIdIntegrationTest {

  @Autowired private NativeService service;

  @Autowired private RightRepository rightRepository;
  @Autowired private TestBaseUserRepository testUserRepository;

  @Autowired private TestingUtils testingUtils;

  @Test
  void testUniqueId() {
    final Native saved1 = service.create(new NativeDTO("test"));

    Right right = rightRepository.save(Right.builder().authority("test").build());
    TestUser user = testUserRepository.save(testingUtils.createRandomUser());

    final Native saved2 = service.create(new NativeDTO("another"));

    Assertions.assertThat(saved1.getId()).isEqualTo(1L);
    Assertions.assertThat(user.getId()).isGreaterThan(1L);
    Assertions.assertThat(saved2.getId()).isEqualTo(2L);

    rightRepository.delete(right);
  }
}
