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

package de.frachtwerk.essencium.backend.configuration.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DefaultRightInitializerTest {

  private final RightService rightServiceMock = mock(RightService.class);

  @BeforeEach
  void setUp() {
    reset(rightServiceMock);
  }

  @Test
  void testInitializeRights() {
    final var sut = new DefaultRightInitializer(rightServiceMock);

    when(rightServiceMock.getAll())
        .thenReturn(List.of(new Right(BasicApplicationRight.API_DEVELOPER.name(), "")));

    sut.run();

    final var capture = ArgumentCaptor.forClass(Right.class);
    verify(rightServiceMock, times(BasicApplicationRight.values().length - 1))
        .create(capture.capture());
    final var updatedRightAuthorities =
        capture.getAllValues().stream().map(Right::getAuthority).collect(Collectors.toList());

    assertThat(updatedRightAuthorities)
        .containsExactlyInAnyOrder(
            Stream.of(BasicApplicationRight.values())
                .filter(
                    basicApplicationRight ->
                        !basicApplicationRight
                            .getAuthority()
                            .equals(BasicApplicationRight.API_DEVELOPER.name()))
                .map(BasicApplicationRight::getAuthority)
                .toArray(String[]::new));
  }

  @Test
  void testGetBasicApplicationRights() {
    final var sut = new DefaultRightInitializer(rightServiceMock);
    assertThat(
            sut.getBasicApplicationRights().stream()
                .map(r -> ((Right) r).getAuthority())
                .collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(
            Stream.of(BasicApplicationRight.values())
                .map(BasicApplicationRight::getAuthority)
                .toArray(String[]::new));
  }

  @Test
  void testGetAdditionalApplicationRights() {
    final var sut = new DefaultRightInitializer(rightServiceMock);

    assertThat(sut.getAdditionalApplicationRights()).isEmpty();
  }

  @Test
  void testConflictingAuthorities() {
    when(rightServiceMock.getAll()).thenReturn(List.of());

    class TestRightInitializer extends DefaultRightInitializer {
      public TestRightInitializer() {
        super(rightServiceMock);
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
  void testDeleteExistingRight() {
    final var existingRight1 = mock(Right.class);
    when(existingRight1.getAuthority()).thenReturn("authority");
    when(rightServiceMock.getAll()).thenReturn(List.of(existingRight1));

    final var sut = new DefaultRightInitializer(rightServiceMock);
    sut.run();

    verify(rightServiceMock, times(1))
        .deleteByAuthority(Objects.requireNonNull(existingRight1.getAuthority()));
  }
}
