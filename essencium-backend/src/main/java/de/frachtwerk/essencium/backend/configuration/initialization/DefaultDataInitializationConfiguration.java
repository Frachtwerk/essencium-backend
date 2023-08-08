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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.io.Serializable;
import java.util.List;
import javax.xml.crypto.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultDataInitializationConfiguration<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    implements DataInitializationConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DefaultDataInitializationConfiguration.class);

  private final DefaultTranslationInitializer translationInitializer;
  private final DefaultRightInitializer rightsInitializer;
  private final DefaultRoleInitializer rolesInitializer;
  private final DefaultUserInitializer<USER, USERDTO, ID> usersInitializer;
  private final DataMigrationInitializer<USER, ID> migrationInitializer;

  @Autowired
  public DefaultDataInitializationConfiguration(
      DefaultTranslationInitializer translationInitializer,
      DefaultRightInitializer rightsInitializer,
      DefaultRoleInitializer rolesInitializer,
      DefaultUserInitializer<USER, USERDTO, ID> usersInitializer,
      DataMigrationInitializer<USER, ID> migrationInitializer) {
    LOGGER.info(
        "Using {} as no other {} beans with higher order are present.",
        this.getClass().getSimpleName(),
        Data.class.getSimpleName());

    this.translationInitializer = translationInitializer;
    this.rightsInitializer = rightsInitializer;
    this.rolesInitializer = rolesInitializer;
    this.usersInitializer = usersInitializer;
    this.migrationInitializer = migrationInitializer;
  }

  @Override
  public List<DataInitializer> getInitializers() {
    return List.of(
        translationInitializer,
        rightsInitializer,
        rolesInitializer,
        usersInitializer,
        migrationInitializer);
  }
}
