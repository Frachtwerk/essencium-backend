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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStateService {

  private final BaseUserRepository baseUserRepository;

  @PersistenceContext private EntityManager entityManager;

  public UserStateService(BaseUserRepository baseUserRepository) {
    this.baseUserRepository = baseUserRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<AbstractBaseUser<?>> fetchOriginalUserState(AbstractBaseUser<?> user) {
    try {
      entityManager.clear();
      AbstractBaseUser<?> originalUser =
          (AbstractBaseUser<?>) baseUserRepository.findById(user.getId()).orElse(null);

      if (Objects.nonNull(originalUser)) {
        entityManager.detach(originalUser);
      }
      return Optional.ofNullable(originalUser);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
