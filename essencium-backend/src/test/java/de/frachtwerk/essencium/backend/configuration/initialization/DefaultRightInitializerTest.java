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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRightInitializerTest {
  @Mock RightService rightServiceMock;
  @Mock RoleRepository roleRepositoryMock;
  @InjectMocks DefaultRightInitializer SUT;

  @BeforeEach
  void setUp() {
    reset(rightServiceMock, roleRepositoryMock);
  }

  @Test
  void testInitializeRights() {
    when(rightServiceMock.getAll())
        .thenReturn(List.of(new Right(BasicApplicationRight.API_DEVELOPER.name(), "")));

    SUT.run();

    final var capture = ArgumentCaptor.forClass(Right.class);
    verify(rightServiceMock, times(BasicApplicationRight.values().length - 1))
        .save(capture.capture());
    final var updatedRightAuthorities =
        capture.getAllValues().stream().map(Right::getAuthority).collect(Collectors.toList());

    assertThat(updatedRightAuthorities)
        .containsExactlyInAnyOrder(
            Stream.of(BasicApplicationRight.values())
                .map(BasicApplicationRight::getAuthority)
                .filter(authority -> !authority.equals(BasicApplicationRight.API_DEVELOPER.name()))
                .toArray(String[]::new));
  }

  @Test
  void testGetBasicApplicationRights() {
    assertThat(
            SUT.getBasicApplicationRights().stream()
                .map(Right::getAuthority)
                .collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(
            Stream.of(BasicApplicationRight.values())
                .map(BasicApplicationRight::getAuthority)
                .toArray(String[]::new));
  }

  @Test
  void testGetAdditionalApplicationRights() {
    assertThat(SUT.getAdditionalApplicationRights()).isEmpty();
  }

  @Test
  void testConflictingAuthorities() {
    when(rightServiceMock.getAll()).thenReturn(List.of());

    class TestRightInitializer extends DefaultRightInitializer {
      public TestRightInitializer() {
        super(rightServiceMock, roleRepositoryMock);
      }

      @Override
      public Set<Right> getAdditionalApplicationRights() {
        return getBasicApplicationRights();
      }
    }

    final var sut = new TestRightInitializer();

    assertThrows(IllegalStateException.class, sut::run);
    verify(rightServiceMock, times(1)).getAll();
    verifyNoMoreInteractions(rightServiceMock);
  }

  @Test
  void deleteExistingRightsTest() {
    final var existingRight1 = mock(Right.class);
    Role role = Role.builder().name("ROLE").rights(new HashSet<>(List.of(existingRight1))).build();
    when(existingRight1.getAuthority()).thenReturn("authority");
    when(rightServiceMock.getAll()).thenReturn(List.of(existingRight1));
    when(roleRepositoryMock.findAllByRights_Authority(any())).thenReturn(List.of(role));

    SUT.run();

    int basicApplicationRightsCount =
        Long.valueOf(Arrays.stream(BasicApplicationRight.values()).count()).intValue();
    verify(rightServiceMock, times(basicApplicationRightsCount)).save(any(Right.class));
    verify(rightServiceMock, times(1))
        .deleteByAuthority(Objects.requireNonNull(existingRight1.getAuthority()));
    verify(roleRepositoryMock, times(1)).save(any(Role.class));
    verifyNoMoreInteractions(rightServiceMock, roleRepositoryMock);
  }

  @Test
  void getCombinedRightsStringTest() {
    assertThat(SUT.getCombinedRights(Stream.of("CREATE", "READ", "UPDATE", "DELETE"), "EXAMPLE"))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                new Right("EXAMPLE_CREATE", ""),
                new Right("EXAMPLE_READ", ""),
                new Right("EXAMPLE_UPDATE", ""),
                new Right("EXAMPLE_DELETE", "")));
  }

  @Test
  void getCombinedRightsRightTest() {
    assertThat(
            SUT.getCombinedRights(
                Stream.of("CREATE", "READ", "UPDATE", "DELETE"),
                Right.builder().authority("EXAMPLE").build()))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                new Right("EXAMPLE_CREATE", ""),
                new Right("EXAMPLE_READ", ""),
                new Right("EXAMPLE_UPDATE", ""),
                new Right("EXAMPLE_DELETE", "")));
  }
}
