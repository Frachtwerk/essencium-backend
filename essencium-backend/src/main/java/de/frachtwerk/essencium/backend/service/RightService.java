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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RightService {

  private final RightRepository repository;
  private final RoleService roleService;

  protected RightService(@NotNull final RightRepository repository, RoleService roleService) {
    this.repository = repository;
    this.roleService = roleService;
  }

  public void deleteByAuthority(String authority) {
    roleService
        .getByRight(authority)
        .forEach(
            role ->
                roleService.patch(
                    Objects.requireNonNull(role.getName()),
                    new HashMap<>(
                        Map.of(
                            "rights",
                            role.getRights().stream()
                                .filter(r -> !r.getAuthority().equals(authority))
                                .collect(Collectors.toSet())))));
    repository.deleteById(authority);
  }

  public List<Right> getAll() {
    return repository.findAll();
  }

  public Page<Right> getAll(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public void create(Right right) {
    repository.save(right);
  }

  public void update(String authority, Right right) {
    if (!Objects.equals(right.getAuthority(), authority)) {
      throw new ResourceUpdateException("ID needs to match entity ID");
    }
    if (!repository.existsById(authority)) {
      throw new ResourceNotFoundException("Entity to update is not persistent");
    }
    repository.save(right);
  }
}
