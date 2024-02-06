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

package de.frachtwerk.essencium.backend.security.permission;

import java.io.Serializable;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DelegatingPermissionEvaluator implements PermissionEvaluator {

  private final NoopPermissionEvaluator noopPermissionEvaluator;
  private final Collection<EntityPermissionEvaluator> availableEvaluators;

  @Autowired
  public DelegatingPermissionEvaluator(Collection<EntityPermissionEvaluator> availableEvaluators) {
    this.availableEvaluators = availableEvaluators;
    this.noopPermissionEvaluator = new NoopPermissionEvaluator();
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    return findEvaluator(targetDomainObject)
        .hasPermission(authentication, targetDomainObject, permission);
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return findEvaluator(targetType)
        .hasPermission(authentication, targetId, targetType, permission);
  }

  private PermissionEvaluator findEvaluator(Object targetEntity) {
    return availableEvaluators.stream()
        .filter(e -> e.supports(targetEntity.getClass()))
        .findFirst()
        .map(e -> (PermissionEvaluator) e)
        .orElse(noopPermissionEvaluator);
  }

  private PermissionEvaluator findEvaluator(String targetType) {
    return availableEvaluators.stream()
        .filter(e -> e.supports(targetType))
        .findFirst()
        .map(e -> (PermissionEvaluator) e)
        .orElse(noopPermissionEvaluator);
  }
}
