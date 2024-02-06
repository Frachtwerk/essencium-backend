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

package de.frachtwerk.essencium.backend.service.initialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.configuration.initialization.DataInitializationConfiguration;
import de.frachtwerk.essencium.backend.configuration.initialization.DataInitializer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class DataInitializationServiceTest {

  @Mock private DataInitializationConfiguration dataInitializationConfiguration;

  DataInitializationServiceTest() {
    dataInitializationConfiguration = mock(DataInitializationConfiguration.class);
  }

  @Test
  void testInitializersAreCalledInOrder() {
    final var sut = new DataInitializationService(dataInitializationConfiguration);

    final Queue<DataInitializer> callQueue = new ConcurrentLinkedQueue<>();

    final var initializer1 = new SelfRefInitializer(10, callQueue);
    final var initializer2 = new SelfRefInitializer(20, callQueue);
    final var initializer3 = new SelfRefInitializer(30, callQueue);

    when(dataInitializationConfiguration.getInitializers())
        .thenReturn(List.of(initializer2, initializer1, initializer3));

    sut.initialize();

    assertThat(callQueue, Matchers.contains(initializer1, initializer2, initializer3));
  }

  @Test
  void testNotFailingOnNullReturn() {
    final var sut = new DataInitializationService(dataInitializationConfiguration);

    when(dataInitializationConfiguration.getInitializers()).thenReturn(null);

    assertDoesNotThrow(sut::initialize);
  }

  static class SelfRefInitializer implements DataInitializer {

    private final int order;
    private final Queue<DataInitializer> callQueue;

    SelfRefInitializer(int order, Queue<DataInitializer> callQueue) {
      this.order = order;
      this.callQueue = callQueue;
    }

    @Override
    public void run() {
      callQueue.add(this);
    }

    @Override
    public int order() {
      return order;
    }
  }
}
