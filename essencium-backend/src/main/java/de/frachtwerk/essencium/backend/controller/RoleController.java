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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/roles")
@Tag(
    name = "RoleController",
    description =
        "Set of endpoints to manage global application roles and their respective rights / permissions")
public class RoleController {

  private final RoleService roleService;

  public RoleController(final RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  @PreAuthorize("hasPermission(null, 'Role', 'read')")
  @Operation(description = "List all available roles, including their rights")
  public Page<Role> findAll(@NotNull final Pageable pageable) {
    return roleService.getAll(pageable);
  }

  @GetMapping(value = "/{id}")
  @PreAuthorize("hasPermission(#id, 'Role', 'read')")
  @Operation(description = "Retrieve a specific role by its id")
  public Role findById(@PathVariable("id") @NotNull final String id) {
    return roleService.getById(id);
  }

  @PostMapping
  @PreAuthorize("hasPermission(#role, 'create')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(description = "Create a new role")
  public Role create(@Valid @RequestBody @NotNull final RoleDto role) {
    if (roleService.getRole(role.getName()).isPresent()) {
      throw new DuplicateResourceException("already existing");
    }
    return roleService.create(role);
  }

  @PutMapping(value = "/{id}")
  @PreAuthorize("hasPermission(#id, 'Role', 'update')")
  @Operation(description = "Update a given role by passing an entire update object")
  public Role updateObject(
      @PathVariable("id") @NotNull final String id,
      @Valid @RequestBody @NotNull final RoleDto role) {
    return roleService.update(id, role);
  }

  @PatchMapping(value = "/{id}")
  @PreAuthorize("hasPermission(#id, 'Role', 'update')")
  @Operation(description = "Update a given role by passing individual fields")
  public Role update(
      @PathVariable("id") final String id,
      @NotNull @RequestBody final Map<String, Object> roleFields) {
    return roleService.patch(id, roleFields);
  }

  @DeleteMapping(value = "/{id}")
  @PreAuthorize("hasPermission(#id, 'Role', 'delete')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(description = "Delete a given role by its id")
  public void delete(@PathVariable("id") @NotNull final String id) {
    roleService.deleteById(id);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(
        HttpMethod.GET,
        HttpMethod.HEAD,
        HttpMethod.POST,
        HttpMethod.PUT,
        HttpMethod.PATCH,
        HttpMethod.DELETE,
        HttpMethod.OPTIONS);
  }
}
