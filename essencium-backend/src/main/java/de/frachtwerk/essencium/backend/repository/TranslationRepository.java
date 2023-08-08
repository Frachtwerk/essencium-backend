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

package de.frachtwerk.essencium.backend.repository;

import de.frachtwerk.essencium.backend.model.Translation;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TranslationRepository
    extends JpaRepository<Translation, Translation.TranslationId> {

  List<Translation> findAllByLocale(@NotNull Locale locale);

  @Query("SELECT DISTINCT t.locale FROM Translation t")
  Set<Locale> findDistinctLocale();

  List<Translation> findAllByKey(String key);

  Optional<Translation> findByKeyAndLocale(String key, Locale locale);

  @Transactional
  @Modifying
  @Query("UPDATE Translation t SET t.locale = ?1 WHERE t.locale = ?2")
  void migrateLocales(Locale to, Locale from);
}
