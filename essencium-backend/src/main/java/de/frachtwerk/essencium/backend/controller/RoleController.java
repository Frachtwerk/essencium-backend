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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
  @Parameter(
      in = ParameterIn.QUERY,
      description = "Page you want to retrieve (0..N)",
      name = "page",
      content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
  @Parameter(
      in = ParameterIn.QUERY,
      description = "Number of records per page.",
      name = "size",
      content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
  @Parameter(
      in = ParameterIn.QUERY,
      description =
          "Sorting criteria in the format: property(,)(asc|desc). "
              + "Default sort order is ascending. "
              + "Multiple sort criteria are supported.",
      name = "sort",
      content = @Content(array = @ArraySchema(schema = @Schema(type = "string"))))
  @Secured("ROLE_READ")
  @Operation(description = "List all available roles, including their rights")
  public Page<Role> findAll(@NotNull @ParameterObject final Pageable pageable) {
    return roleService.getAll(pageable);
  }

  @GetMapping(value = "/{name}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "name",
      description = "Name of the role to retrieve",
      content = @Content(schema = @Schema(type = "string")),
      required = true)
  @Secured("ROLE_READ")
  @Operation(description = "Retrieve a specific role by its name")
  public Role findById(@PathVariable("name") @NotNull final String id) {
    return roleService.getByName(id);
  }

  @PostMapping
  @Secured(value = "ROLE_CREATE")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(description = "Create a new role")
  public Role create(@Valid @RequestBody @NotNull final RoleDto role) {
    return roleService.create(role);
  }

  @PutMapping(value = "/{name}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "name",
      description = "Name of the role to be updated",
      content = @Content(schema = @Schema(type = "string")),
      required = true)
  @Secured("ROLE_UPDATE")
  @Operation(description = "Update a given role by passing an entire update object")
  public Role update(
      @PathVariable("name") @NotNull final String name,
      @Valid @RequestBody @NotNull final RoleDto role) {
    return roleService.update(name, role);
  }

  @PatchMapping(value = "/{name}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "name",
      description = "Name of the role to be updated",
      content = @Content(schema = @Schema(type = "string")),
      required = true)
  @Secured("ROLE_UPDATE")
  @Operation(description = "Update a given role by passing individual fields")
  public Role patch(
      @PathVariable("name") final String name,
      @NotNull @RequestBody final Map<String, Object> roleFields) {
    return roleService.patch(name, roleFields);
  }

  @DeleteMapping(value = "/{name}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "name",
      description = "Name of the role to be deleted",
      content = @Content(schema = @Schema(type = "string")),
      required = true)
  @Secured("ROLE_DELETE")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(description = "Delete a given role by its id")
  public void delete(@PathVariable("name") @NotNull final String name) {
    roleService.deleteById(name);
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
