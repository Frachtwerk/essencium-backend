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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;
import javax.xml.crypto.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DefaultDataInitializationConfiguration<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    implements DataInitializationConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DefaultDataInitializationConfiguration.class);

  private final DefaultTranslationInitializer defaultTranslationInitializer;
  private final DefaultRightInitializer defaultRightInitializer;
  private final DefaultRoleInitializer defaultRoleInitializer;
  private final DefaultUserInitializer<USER, USERDTO, ID> defaultUserInitializer;
  private final DataMigrationInitializer<USER, ID> dataMigrationInitializer;

  @PostConstruct
  public void postConstruct() {
    LOGGER.info(
        "Using {} as no other {} beans with higher order are present.",
        this.getClass().getSimpleName(),
        Data.class.getSimpleName());
  }

  @Override
  public List<DataInitializer> getInitializers() {
    return List.of(
        dataMigrationInitializer, // 5
        defaultTranslationInitializer, // 10
        defaultRightInitializer, // 20
        defaultRoleInitializer, // 30
        defaultUserInitializer); // 40
  }
}
