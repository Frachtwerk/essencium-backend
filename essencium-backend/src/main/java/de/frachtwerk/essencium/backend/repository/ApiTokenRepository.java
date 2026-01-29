/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.ApiTokenStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTokenRepository extends BaseRepository<ApiToken, UUID> {
  List<ApiToken> findAllByLinkedUser(String linkedUser);

  @Modifying
  @Query(
      "update ApiToken at set at.status = :status, at.validUntil = :validUntil where at.id = :id")
  void setStatusAndExpirationById(ApiTokenStatus status, LocalDate validUntil, UUID id);

  List<ApiToken> findAllByStatusAndValidUntilBefore(ApiTokenStatus status, LocalDate validUntil);

  @Modifying
  @Query("update ApiToken at set at.status = :status where at.id in :ids")
  void setStatusByIds(ApiTokenStatus status, List<UUID> ids);

  @Modifying
  @Query("delete from ApiToken at where at.validUntil < :validUntil")
  void deleteAllByValidUntilBefore(LocalDate validUntil);

  @Query("select at.id from ApiToken at join at.rights r where r.authority = :rightName")
  List<UUID> findAllByRightName(String rightName);
}
