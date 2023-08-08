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
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import java.io.Serializable;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataMigrationInitializer<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    implements DataInitializer {
  private final TranslationRepository translationRepository;
  private final BaseUserRepository<USER, ID> userRepository;

  @Autowired
  public DataMigrationInitializer(
      TranslationRepository translationRepository, BaseUserRepository<USER, ID> userRepository) {
    this.translationRepository = translationRepository;
    this.userRepository = userRepository;
  }

  @Override
  public void run() {
    translationRepository.migrateLocales(Locale.GERMAN, Locale.GERMANY);
    translationRepository.migrateLocales(Locale.ENGLISH, Locale.US);
    userRepository.migrateUserLocales(Locale.GERMAN, Locale.GERMANY);
    userRepository.migrateUserLocales(Locale.ENGLISH, Locale.US);
  }

  @Override
  public int order() {
    return 5;
  }
}
