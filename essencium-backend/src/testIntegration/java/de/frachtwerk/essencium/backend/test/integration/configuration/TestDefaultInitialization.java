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

package de.frachtwerk.essencium.backend.test.integration.configuration;

import static de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer.ADMIN_ROLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.repository.TestBaseUserRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class TestDefaultInitialization {

  public static final String ADMIN_USERNAME = "devnull@frachtwerk.de";

  @Autowired private RightRepository rightRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TestBaseUserRepository userRepository;
  @Autowired private TranslationRepository translationRepository;

  @Test
  void testAdminUserInitialization() {
    final List<Right> initializedRights = rightRepository.findAll();
    final var adminRole =
        roleRepository.findByName(ADMIN_ROLE_NAME).orElseThrow(AssertionError::new);
    final var adminUser =
        userRepository.findByEmailIgnoreCase(ADMIN_USERNAME).orElseThrow(AssertionError::new);

    final var expectedRightAuthorities =
        Stream.of(BasicApplicationRight.values())
            .map(BasicApplicationRight::getAuthority)
            .collect(Collectors.toSet());

    assertThat(initializedRights.stream().map(Right::getAuthority))
        .containsExactlyInAnyOrderElementsOf(expectedRightAuthorities);

    assertThat(adminRole.getRights().stream().map(Right::getAuthority).collect(Collectors.toSet()))
        .containsExactlyInAnyOrderElementsOf(expectedRightAuthorities);

    assertThat(adminUser.isEnabled()).isTrue();
    assertThat(adminUser.getRole()).isEqualTo(adminRole);
  }

  @Test
  void testTranslationInitialization() {
    final var germanTranslations = translationRepository.findAllByLocale(Locale.GERMAN);
    final var usTranslations = translationRepository.findAllByLocale(Locale.ENGLISH);

    assertThat(germanTranslations).hasSizeGreaterThan(20);
    assertThat(usTranslations).hasSizeGreaterThan(20);
  }
}
