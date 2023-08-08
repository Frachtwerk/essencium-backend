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

package de.frachtwerk.essencium.backend.service.initialization;

import de.frachtwerk.essencium.backend.configuration.initialization.DataInitializationConfiguration;
import de.frachtwerk.essencium.backend.configuration.initialization.DataInitializer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@Configuration
public class DataInitializationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializationService.class);

  private final DataInitializationConfiguration initializationConfiguration;

  @Autowired
  DataInitializationService(DataInitializationConfiguration initializationConfiguration) {
    this.initializationConfiguration = initializationConfiguration;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(100000)
  public final void initialize() {
    LOGGER.info("Running data initialization with {} initializers", getInitializers().size());

    getInitializers().stream()
        .sorted(Comparator.comparing(DataInitializer::order))
        .forEach(DataInitializer::run);

    LOGGER.info("Database initialization completed");
  }

  private List<DataInitializer> getInitializers() {
    return Optional.ofNullable(initializationConfiguration.getInitializers()).orElse(List.of());
  }
}
