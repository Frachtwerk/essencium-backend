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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.ResourceActions;
import de.frachtwerk.essencium.backend.model.representation.BasicRepresentation;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/v1/users")
@Tag(
    name = "UserController",
    description = "Set of endpoints to manage system users, including yourself")
public abstract class AbstractUserController<
        USER extends AbstractBaseUser<ID>,
        REPRESENTATION,
        USERDTO extends UserDto<ID>,
        SPEC extends BaseUserSpec<USER, ID>,
        ID extends Serializable>
    extends AbstractAccessAwareController<USER, ID, USERDTO, REPRESENTATION, SPEC> {

  protected static final Set<String> PROTECTED_USER_FIELDS =
      Set.of("source", "nonce", "passwordResetToken");

  protected final AbstractRepresentationAssembler<USER, REPRESENTATION> assembler;

  protected final AbstractUserService<USER, ID, USERDTO> userService;

  protected AbstractUserController(
      AbstractUserService<USER, ID, USERDTO> userService,
      AbstractRepresentationAssembler<USER, REPRESENTATION> assembler) {
    super(userService);
    this.userService = userService;
    this.assembler = assembler;
  }

  @Override
  @GetMapping
  @Secured({BasicApplicationRight.Authority.USER_READ})
  @Operation(
      summary =
          "Find all users according to certain optional filter parameters and return them as a page of user representations")
  @Parameter(
      in = ParameterIn.QUERY,
      description = "Page you want to retrieve (0..N)",
      name = "page",
      schema = @Schema(type = "integer", defaultValue = "0"))
  @Parameter(
      in = ParameterIn.QUERY,
      description = "Number of records per page.",
      name = "size",
      schema = @Schema(type = "integer", defaultValue = "20"))
  @Parameter(
      in = ParameterIn.QUERY,
      description =
          "Sorting criteria in the format: property(,)(asc|desc). "
              + "Default sort order is ascending. "
              + "Multiple sort criteria are supported.",
      name = "sort",
      array = @ArraySchema(schema = @Schema(type = "string")))
  @Parameter(
      in = ParameterIn.QUERY,
      name = "ids",
      description =
          "IDs of the requested entities. can contain multiple values separated by ','"
              + "Multiple criteria are supported.",
      array = @ArraySchema(schema = @Schema(type = "integer")),
      example = "1,2,5")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdBy",
      description = "full username (email)",
      schema = @Schema(type = "string"),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedBy",
      description = "full username (email)",
      schema = @Schema(type = "string"),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtFrom",
      description = "returns entries created after the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtTo",
      description = "returns entries created before the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-12-31T23:59:59")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtFrom",
      description = "returns entries updated after the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtTo",
      description = "returns entries updated before the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-12-31T23:59:59")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "roles",
      description = "A Role ID or name to filter by",
      array = @ArraySchema(schema = @Schema(type = "integer")),
      example = "1,2,5")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "name",
      description = "A firstName or lastName to filter by",
      schema = @Schema(type = "string"),
      example = "Peter")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "email",
      description = "An email address to filter by",
      schema = @Schema(type = "string"),
      example = "john.doe@frachtwerk.de")
  public Page<REPRESENTATION> findAll(
      @Parameter(hidden = true) SPEC specification, @ParameterObject final Pageable pageable) {
    return super.findAll(specification, pageable);
  }

  @Override
  @GetMapping("/basic")
  @Secured({BasicApplicationRight.Authority.USER_READ})
  @Operation(
      summary =
          "Find all users according to certain optional filter parameters and returns all as list of basic representations")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "ids",
      description =
          "IDs of the requested entities. can contain multiple values separated by ','"
              + "Multiple criteria are supported.",
      array = @ArraySchema(schema = @Schema(type = "integer")),
      example = "1,2,5")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdBy",
      description = "full username (email)",
      schema = @Schema(type = "string"),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedBy",
      description = "full username (email)",
      schema = @Schema(type = "string"),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtFrom",
      description = "returns entries created after the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtTo",
      description = "returns entries created before the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-12-31T23:59:59")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtFrom",
      description = "returns entries updated after the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtTo",
      description = "returns entries updated before the submitted date and time ",
      schema = @Schema(type = "string", format = "date-time"),
      example = "2021-12-31T23:59:59")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "roles",
      description = "A Role ID or name to filter by",
      array = @ArraySchema(schema = @Schema(type = "integer")),
      example = "1,2,5")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "name",
      description = "A firstName or lastName to filter by",
      schema = @Schema(type = "string"),
      example = "Peter")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "email",
      description = "An email address to filter by",
      schema = @Schema(type = "string"),
      example = "john.doe@frachtwerk.de")
  public List<BasicRepresentation> findAll(@Parameter(hidden = true) SPEC specification) {
    return super.findAll(specification);
  }

  @Override
  @GetMapping(value = "/{id}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the user to retrieve",
      required = true,
      schema = @Schema(type = "integer"))
  @Secured({BasicApplicationRight.Authority.USER_READ})
  @Operation(summary = "Retrieve a user by her id")
  public REPRESENTATION findById(
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class) SPEC spec,
      @PathVariable ID id) {
    return super.findById(spec, id);
  }

  @Override
  @PostMapping
  @Secured({BasicApplicationRight.Authority.USER_CREATE})
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new user")
  public REPRESENTATION create(@NotNull @Valid @RequestBody final USERDTO user) {
    try {
      userService.loadUserByUsername(user.getEmail());
    } catch (UsernameNotFoundException e) {
      return assembler.toModel(userService.create(user));
    }
    throw new DuplicateResourceException(
        user.getClass().getSimpleName(), ResourceActions.CREATE.toString(), user.getEmail());
  }

  @Override
  @PutMapping(value = "/{id}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the user to update",
      required = true,
      schema = @Schema(type = "integer"))
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @Operation(summary = "Update a user by passing the entire object")
  public REPRESENTATION update(
      @PathVariable("id") @NotNull final ID id,
      @Valid @RequestBody @NotNull final USERDTO user,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    return super.update(id, user, spec);
  }

  @Override
  @PatchMapping(value = "/{id}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the user to update",
      required = true,
      schema = @Schema(type = "integer"))
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @Operation(summary = "Update a user by passing individual fields")
  public REPRESENTATION update(
      @PathVariable("id") @NotNull final ID id,
      @RequestBody @NotNull Map<String, Object> userFields,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    userFields =
        userFields.entrySet().stream()
            .filter(e -> !PROTECTED_USER_FIELDS.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return super.update(id, userFields, spec);
  }

  @Override
  @DeleteMapping(value = "/{id}")
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the user to delete",
      required = true,
      schema = @Schema(type = "string"))
  @Secured({BasicApplicationRight.Authority.USER_DELETE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a user by her id")
  public void delete(
      @PathVariable("id") @NotNull final ID id,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    super.delete(id, spec);
  }

  @PostMapping(value = "/{id}/terminate")
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the user to terminate",
      required = true,
      schema = @Schema(type = "string"))
  @Secured({BasicApplicationRight.Authority.USER_UPDATE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary =
          "Terminate all sessions of the given user, i.e. invalidate her tokens to effectively log the user out")
  public void terminate(
      @PathVariable @NotNull final ID id,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    super.update(id, Map.of("nonce", AbstractUserService.generateNonce()), spec);
  }

  // Current user-related endpoints

  @GetMapping("/me")
  @Operation(summary = "Retrieve the currently logged-in user")
  public REPRESENTATION getMe(@Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return toRepresentation(user);
  }

  @PutMapping("/me")
  @Operation(summary = "Update the currently logged-in user by passing the entire update object")
  public REPRESENTATION updateMe(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @Valid @NotNull @RequestBody final USERDTO updateInformation) {
    return toRepresentation(userService.selfUpdate(user, updateInformation));
  }

  @PatchMapping("/me")
  @Operation(summary = "Update the currently logged-in user by passing individual fields")
  public REPRESENTATION updateMePartial(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @NotNull @RequestBody final Map<String, Object> userFields) {
    return toRepresentation(userService.selfUpdate(user, userFields));
  }

  @PutMapping("/me/password")
  @Operation(summary = "Change the currently logged-in user's password")
  public REPRESENTATION updatePassword(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @NotNull @Valid @RequestBody final PasswordUpdateRequest updateRequest) {
    return toRepresentation(userService.updatePassword(user, updateRequest));
  }

  /**
   * @deprecated Use {@link #getMyRole(USER)} ("/me/roles") instead
   * @param user {@link USER}
   * @return {@link Set<Role>}
   */
  @Deprecated(since = "2.5.0", forRemoval = true)
  @GetMapping("/me/role")
  @Hidden
  @Operation(summary = "Retrieve the currently logged-in user's role")
  public Set<Role> getMyRoleOld(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getRoles();
  }

  /**
   * @deprecated Use {@link #getMyRights(USER)} ("/me/roles/rights") instead
   * @param user {@link USER}
   * @return {@link Collection<GrantedAuthority>}
   */
  @Deprecated(since = "2.5.0", forRemoval = true)
  @GetMapping("/me/role/rights")
  @Hidden
  @Operation(summary = "Retrieve the currently logged-in user's rights / permissions")
  public Collection<GrantedAuthority> getMyRightsOld(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getAuthorities();
  }

  @GetMapping("/me/roles")
  @Operation(summary = "Retrieve the currently logged-in user's role")
  public Set<Role> getMyRole(@Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getRoles();
  }

  @GetMapping("/me/roles/rights")
  @Operation(summary = "Retrieve the currently logged-in user's rights / permissions")
  public Collection<GrantedAuthority> getMyRights(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user) {
    return user.getAuthorities();
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
  @Parameter(
      in = ParameterIn.PATH,
      name = "id",
      description = "ID of the token to delete",
      required = true,
      schema = @Schema(type = "string"))
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Retrieve refresh tokens of the currently logged-in user")
  public void deleteToken(
      @Parameter(hidden = true) @AuthenticationPrincipal final USER user,
      @PathVariable("id") @NotNull final UUID id) {
    userService.deleteToken(user, id);
  }

  @Override
  protected REPRESENTATION toRepresentation(USER entity) {
    return assembler.toModel(entity);
  }

  @Override
  protected Page<REPRESENTATION> toRepresentation(Page<USER> page) {
    return page.map(this::toRepresentation);
  }
}
