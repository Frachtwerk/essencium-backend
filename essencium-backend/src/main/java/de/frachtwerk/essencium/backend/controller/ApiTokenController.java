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

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.controller.access.ApiTokenEquals;
import de.frachtwerk.essencium.backend.controller.access.ExposesEntity;
import de.frachtwerk.essencium.backend.controller.access.OwnershipSpec;
import de.frachtwerk.essencium.backend.controller.access.RestrictAccessToOwnedEntities;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenDto;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.BasicRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.ApiTokenSpecification;
import de.frachtwerk.essencium.backend.security.AdditionalApplicationRights;
import de.frachtwerk.essencium.backend.service.ApiTokenService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.MethodNotAllowedException;

@RestController
@RequestMapping("/v1/api-tokens")
@ExposesEntity(ApiToken.class)
@Tag(name = "ApiTokenController", description = "CRUD operations for API tokens")
public class ApiTokenController
    extends AbstractAccessAwareController<
        ApiToken, UUID, ApiTokenDto, ApiTokenRepresentation, ApiTokenSpecification> {

  private final AppJwtProperties appJwtProperties;

  public ApiTokenController(ApiTokenService service, AppJwtProperties appJwtProperties) {
    super(service);
    this.appJwtProperties = appJwtProperties;
  }

  @Override
  @GetMapping
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  @RestrictAccessToOwnedEntities(
      rights = {
        AdditionalApplicationRights.Authority.API_TOKEN,
        AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
      })
  @OwnershipSpec(path = "linkedUser", userAttribute = "email", spec = Equal.class)
  public Page<ApiTokenRepresentation> findAll(
      ApiTokenSpecification specification, Pageable pageable) {
    return super.findAll(specification, pageable);
  }

  @Override
  @GetMapping("/basic")
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  @RestrictAccessToOwnedEntities(
      rights = {
        AdditionalApplicationRights.Authority.API_TOKEN,
        AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
      })
  @OwnershipSpec(path = "linkedUser", userAttribute = "email", spec = Equal.class)
  public List<BasicRepresentation> findAll(
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true)
          ApiTokenSpecification specification) {
    return super.findAll(specification);
  }

  @GetMapping("/all")
  @Secured({AdditionalApplicationRights.Authority.API_TOKEN_ADMIN})
  public Map<BasicRepresentation, List<ApiTokenRepresentation>> findAllAdmin(
      @Parameter(hidden = true) ApiTokenSpecification specification) {
    List<ApiToken> allFiltered = service.getAllFiltered(specification);
    return allFiltered.stream()
        .map(token -> ((ApiTokenService) service).getAssembler().toModel(token))
        .collect(Collectors.groupingBy(ApiTokenRepresentation::getLinkedUser));
  }

  @Override
  @GetMapping("/{id}")
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  @OwnershipSpec(path = "linkedUser", userAttribute = "email", spec = ApiTokenEquals.class)
  @RestrictAccessToOwnedEntities(rights = {AdditionalApplicationRights.Authority.API_TOKEN})
  public ApiTokenRepresentation findById(
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true)
          ApiTokenSpecification apiTokenSpecification) {
    return super.findById(apiTokenSpecification);
  }

  @Override
  @PostMapping
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  public ApiTokenRepresentation create(@RequestBody @NotNull @Valid ApiTokenDto apiTokenDto) {
    return super.create(apiTokenDto);
  }

  @Override
  @PutMapping("/{id}")
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  @Hidden
  public ApiTokenRepresentation update(
      @PathVariable("id") @NotNull UUID uuid,
      @RequestBody @NotNull @Valid ApiTokenDto apiTokenDto,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true)
          ApiTokenSpecification apiTokenSpecification) {
    throw new MethodNotAllowedException("Method not allowed", getAllowedMethods());
  }

  @Override
  @PatchMapping("/{id}")
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN,
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN
  })
  @RestrictAccessToOwnedEntities(rights = {AdditionalApplicationRights.Authority.API_TOKEN})
  public ApiTokenRepresentation update(
      @PathVariable("id") @NotNull UUID uuid,
      @RequestBody @NotNull Map<String, Object> userFields,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true)
          ApiTokenSpecification apiTokenSpecification) {
    return super.update(uuid, userFields, apiTokenSpecification);
  }

  @Override
  @DeleteMapping("/{id}")
  @Secured({AdditionalApplicationRights.Authority.API_TOKEN_ADMIN})
  public void delete(
      @PathVariable("id") @NotNull UUID uuid,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true)
          ApiTokenSpecification apiTokenSpecification) {
    super.delete(uuid, apiTokenSpecification);
  }

  @GetMapping("/token-expiration-info")
  @Secured({
    AdditionalApplicationRights.Authority.API_TOKEN_ADMIN,
    AdditionalApplicationRights.Authority.API_TOKEN
  })
  @Operation(summary = "Get token expiration info in seconds")
  public int getTokenExpirationInfo() {
    return appJwtProperties.getDefaultApiTokenExpiration();
  }

  @Override
  protected ApiTokenRepresentation toRepresentation(ApiToken entity) {
    return ((ApiTokenService) service).getAssembler().toModel(entity);
  }

  @Override
  protected Page<ApiTokenRepresentation> toRepresentation(Page<ApiToken> page) {
    return page.map(this::toRepresentation);
  }

  @Override
  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(
        HttpMethod.GET,
        HttpMethod.HEAD,
        HttpMethod.POST,
        HttpMethod.PATCH,
        HttpMethod.DELETE,
        HttpMethod.OPTIONS);
  }
}
