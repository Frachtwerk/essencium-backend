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

package de.frachtwerk.essencium.backend.repository;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface BaseUserRepository<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends BaseRepository<USER, ID> {

  Optional<USER> findByEmailIgnoreCase(@NotNull String email);

  Optional<USER> findByPasswordResetToken(@NotNull String passwordResetToken);

  @Transactional
  @Modifying
  @Query("UPDATE #{#entityName} u SET u.locale = ?1 WHERE u.locale = ?2")
  void migrateUserLocales(Locale to, Locale from);

  @Transactional
  @Modifying
  @Query(
      "UPDATE #{#entityName} u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE lower(u.email) = lower(?1) AND u.loginDisabled = false")
  void incrementFailedLoginAttempts(String username);

  @Transactional
  @Modifying
  @Query(
      "UPDATE #{#entityName} u SET u.loginDisabled = true WHERE lower(u.email) = lower(?1) AND u.failedLoginAttempts >= ?2")
  void disableUsersByFailedLoginAttempts(String username, int amount);

  @Query("SELECT u FROM #{#entityName} u INNER JOIN u.roles role WHERE :roleName = role.name")
  List<USER> findByRoleName(String roleName);
}
