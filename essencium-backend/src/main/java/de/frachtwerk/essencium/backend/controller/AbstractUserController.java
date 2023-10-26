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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/v1/users")
@Tag(
    name = "UserController",
    description = "Set of endpoints to manage system users, including yourself")
public abstract class AbstractUserController<
    USER extends AbstractBaseUser<ID>,
    REPRESENTATION,
    USERDTO extends UserDto<ID>,
    SPEC extends BaseUserSpec<USER, ID>,
    ID extends Serializable> {

  protected static final Set<String> PROTECTED_USER_FIELDS =
      Set.of("source", "nonce", "passwordResetToken");

  protected final AbstractRepresentationAssembler<USER, REPRESENTATION> assembler;

  protected final AbstractUserService<USER, ID, USERDTO> userService;

  protected AbstractUserController(
      AbstractUserService<USER, ID, USERDTO> userService,
      AbstractRepresentationAssembler<USER, REPRESENTATION> assembler) {
    this.userService = userService;
    this.assembler = assembler;
  }

  @GetMapping
  @Secured({BasicApplicationRight.Authority.USER_READ})
  @Operation(summary = "Find all users according to certain optional filter parameters")
  @Parameters({
    @Parameter(
        in = ParameterIn.QUERY,
        description = "Page you want to retrieve (0..N)",
        name = "page",
        content = @Content(schema = @Schema(type = "integer", defaultValue = "0"))),
    @Parameter(
        in = ParameterIn.QUERY,
        description = "Number of records per page.",
        name = "size",
        content = @Content(schema = @Schema(type = "integer", defaultValue = "20"))),
    @Parameter(
        in = ParameterIn.QUERY,
        description =
            "Sorting criteria in the format: property(,asc|desc). "
                + "Default sort order is ascending. "
                + "Multiple sort criteria are supported.",
        name = "sort",
        content = @Content(array = @ArraySchema(schema = @Schema(type = "string")))),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "ids",
        description =
            "IDs of the requested entities. can contain multiple values separated by ','"
                + "Multiple criteria are supported.",
        content = @Content(schema = @Schema(type = "long")),
        example = "1,2,5"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "createdBy",
        description = "full username (email)",
        content = @Content(schema = @Schema(type = "string")),
        example = "admin@frachtwerk.de"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "updatedBy",
        description = "full username (email)",
        content = @Content(schema = @Schema(type = "string")),
        example = "admin@frachtwerk.de"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "createdAtFrom",
        description = "returns entries created after the submitted date and time ",
        content = @Content(schema = @Schema(type = "LocalDateTime")),
        example = "2021-01-01T00:00:01"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "createdAtTo",
        description = "returns entries created before the submitted date and time ",
        content = @Content(schema = @Schema(type = "LocalDateTime")),
        example = "2021-12-31T23:59:59"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "updatedAtFrom",
        description = "returns entries updated after the submitted date and time ",
        content = @Content(schema = @Schema(type = "LocalDateTime")),
        example = "2021-01-01T00:00:01"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "updatedAtTo",
        description = "returns entries updated before the submitted date and time ",
        content = @Content(schema = @Schema(type = "LocalDateTime")),
        example = "2021-12-31T23:59:59"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "role",
        description = "A Role ID or name to filter by",
        content = @Content(schema = @Schema(type = "long")),
        example = "5"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "name",
        description = "A firstName or lastName to filter by",
        content = @Content(schema = @Schema(type = "string")),
        example = "Peter"),
    @Parameter(
        in = ParameterIn.QUERY,
        name = "email",
        description = "An email address to filter by",
        content = @Content(schema = @Schema(type = "string")),
        example = "john.doe@frachtwerk.de"),
  })
  public Page<REPRESENTATION> findAll(
      @Parameter(hidden = true) SPEC specification, @NotNull final Pageable pageable) {
    return userService.getAllFiltered(specification, pageable).map(assembler::toModel);
  }

  @GetMapping(value = "/{id}")
  @Secured({BasicApplicationRight.Authority.USER_READ})
  @Operation(summary = "Retrieve a user by her id")
  public REPRESENTATION findById(@PathVariable("id") @NotNull final ID id) {
    return assembler.toModel(userService.getById(id));
  }

  @PostMapping
  @Secured({BasicApplicationRight.Authority.USER_CREATE})
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new user")
  public REPRESENTATION create(@Valid @RequestBody @NotNull final USERDTO user) {
    try {
      userService.loadUserByUsername(user.getEmail());
    } catch (UsernameNotFoundException e) {
      return assembler.toModel(userService.create(user));
    }
    throw new DuplicateResourceException("already existing");
  }

  @PutMapping(value = "/{id}")
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @Operation(summary = "Update a user by passing the entire object")
  public REPRESENTATION updateObject(
      @PathVariable("id") @NotNull final ID id, @Valid @RequestBody @NotNull final USERDTO user) {
    return assembler.toModel(userService.update(id, user));
  }

  @PatchMapping(value = "/{id}")
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @Operation(summary = "Update a user by passing individual fields")
  public REPRESENTATION update(
      @PathVariable("id") final ID id, @NotNull @RequestBody Map<String, Object> userFields) {
    userFields =
        userFields.entrySet().stream()
            .filter(e -> !PROTECTED_USER_FIELDS.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return assembler.toModel(userService.patch(id, userFields));
  }

  @DeleteMapping(value = "/{id}")
  @Secured({BasicApplicationRight.Authority.USER_DELETE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a user by her id")
  public void delete(@PathVariable("id") @NotNull final ID id) {
    userService.deleteById(id);
  }

  @PostMapping(value = "/{id}/terminate")
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary =
          "Terminate all sessions of the given user, i.e. invalidate her tokens to effectively log the user out")
  public void terminate(@PathVariable @NotNull final ID id) {
    userService.patch(id, Map.of("nonce", AbstractUserService.generateNonce()));
  }

  // Current user-related endpoints

  @GetMapping("/me")
  @Operation(summary = "Retrieve the currently logged-in user")
  public REPRESENTATION getMe(@Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return assembler.toModel(user);
  }

  @PutMapping("/me")
  @Operation(summary = "Update the currently logged-in user by passing the entire update object")
  public REPRESENTATION updateMe(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @Valid @NotNull @RequestBody final USERDTO updateInformation) {
    return assembler.toModel(userService.selfUpdate(user, updateInformation));
  }

  @PatchMapping("/me")
  @Operation(summary = "Update the currently logged-in user by passing individual fields")
  public REPRESENTATION updateMePartial(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @NotNull @RequestBody final Map<String, Object> userFields) {
    return assembler.toModel(userService.selfUpdate(user, userFields));
  }

  @PutMapping("/me/password")
  @Operation(summary = "Change the currently logged-in user's password")
  public REPRESENTATION updatePassword(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @NotNull @Valid @RequestBody final PasswordUpdateRequest updateRequest) {
    return assembler.toModel(userService.updatePassword(user, updateRequest));
  }

  @GetMapping("/me/role")
  @Operation(summary = "Retrieve the currently logged-in user's role")
  public Role getMyRole(@Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getRole();
  }

  @GetMapping("/me/role/rights")
  @Operation(summary = "Retrieve the currently logged-in user's rights / permissions")
  public Collection<Right> getMyRights(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getRole().getRights();
  }

  @GetMapping("/me/token")
  @Operation(summary = "Retrieve refresh tokens of the currently logged-in user")
  public List<TokenRepresentation> getMyTokens(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return userService.getTokens(user).stream()
        .map(
            entity ->
                TokenRepresentation.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .issuedAt(entity.getIssuedAt())
                    .expiration(entity.getExpiration())
                    .userAgent(entity.getUserAgent())
                    .lastUsed(
                        Objects.isNull(entity.getLastUsed())
                            ? null
                            : entity
                                .getLastUsed()
                                .toInstant()
                                .atZone(ZoneOffset.UTC)
                                .toLocalDateTime())
                    .build())
        .toList();
  }

  @DeleteMapping("/me/token/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Retrieve refresh tokens of the currently logged-in user")
  public void deleteToken(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @PathVariable("id") @NotNull final UUID id) {
    userService.deleteToken(user, id);
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
