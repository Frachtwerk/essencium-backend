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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RightService {

  private final RightRepository rightRepository;
  private final RoleService roleService;

  public List<Right> getAll() {
    return rightRepository.findAll();
  }

  public Page<Right> getAll(Pageable pageable) {
    return rightRepository.findAll(pageable);
  }

  public Right getByAuthority(String authority) {
    return rightRepository.findByAuthority(authority);
  }

  public void save(Right right) {
    rightRepository.save(right);
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
    rightRepository.deleteByAuthority(authority);
  }
}
