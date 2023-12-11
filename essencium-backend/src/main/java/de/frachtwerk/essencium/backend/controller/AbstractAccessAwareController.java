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

import de.frachtwerk.essencium.backend.controller.access.RestrictAccessToOwnedEntities;
import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.model.Identifiable;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * * This controller takes advantage of the {@link RestrictAccessToOwnedEntities} annotation. If
 * specified on an inheriting type or on the entity type that is served by this controller the
 * specified restriction is applied to GET, GET /{id}, POST, PUT /{id}, PATCH /{id} and DELETE
 * /{id}.
 *
 * <p>Individual methods can have distinct restrictions by overwriting the according method.
 * Annotate it with {@link RestrictAccessToOwnedEntities}.
 *
 * @param <MODEL> The {@link AbstractBaseModel} type that is served by this controller.
 * @param <INPUT> The input type used for POST and PUT methods.
 * @param <REPRESENTATION> The output type serialized as the response body of the GET, POST, PUT and
 *     PATCH requests.
 * @param <SPEC> The {@link Specification} type used for filtering entities.
 */
@AllArgsConstructor
public abstract class AbstractAccessAwareController<
    MODEL extends AbstractBaseModel<ID>,
    ID extends Serializable,
    INPUT extends Identifiable<ID>,
    REPRESENTATION,
    SPEC extends Specification<MODEL>> {
  protected final AbstractEntityService<MODEL, ID, INPUT> service;

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
          "Sorting criteria in the format: property(,asc|desc). "
              + "Default sort order is ascending. "
              + "Multiple sort criteria are supported.",
      name = "sort",
      content = @Content(array = @ArraySchema(schema = @Schema(type = "string"))))
  @Parameter(
      in = ParameterIn.QUERY,
      name = "ids",
      description =
          "IDs of the requested entities. can contain multiple values separated by ','"
              + "Multiple criteria are supported.",
      content = @Content(schema = @Schema(type = "long")),
      example = "1,2,5")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdBy",
      description = "full username (email)",
      content = @Content(schema = @Schema(type = "string")),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedBy",
      description = "full username (email)",
      content = @Content(schema = @Schema(type = "string")),
      example = "devnull@frachtwerk.de")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtFrom",
      description = "returns entries created after the submitted date and time ",
      content = @Content(schema = @Schema(type = "LocalDateTime")),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAtTo",
      description = "returns entries created before the submitted date and time ",
      content = @Content(schema = @Schema(type = "LocalDateTime")),
      example = "2021-12-31T23:59:59")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtFrom",
      description = "returns entries updated after the submitted date and time ",
      content = @Content(schema = @Schema(type = "LocalDateTime")),
      example = "2021-01-01T00:00:01")
  @Parameter(
      in = ParameterIn.QUERY,
      name = "updatedAtTo",
      description = "returns entries updated before the submitted date and time ",
      content = @Content(schema = @Schema(type = "LocalDateTime")),
      example = "2021-12-31T23:59:59")
  public Page<REPRESENTATION> findAll(
      @Parameter(hidden = true) SPEC specification, Pageable pageable) {
    return toRepresentation(service.getAllFiltered(specification, pageable));
  }

  @GetMapping("/{id}")
  public REPRESENTATION findById(
      @Parameter(hidden = true) @Spec(path = "id", pathVars = "id", spec = Equal.class) SPEC spec) {
    return toRepresentation(service.getOne(spec).orElseThrow(ResourceNotFoundException::new));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public REPRESENTATION create(@NotNull @Valid @RequestBody INPUT input) {
    input.setId(null); // otherwise existing entities can be overwritten
    return toRepresentation(service.create(input));
  }

  @PutMapping("/{id}")
  public REPRESENTATION update(
      @PathVariable("id") @NotNull final ID id,
      @Valid @RequestBody @NotNull final INPUT input,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    return toRepresentation(service.testAccess(spec).update(id, input));
  }

  @PatchMapping(value = "/{id}")
  public REPRESENTATION update(
      @PathVariable("id") @NotNull final ID id,
      @NotNull @RequestBody final Map<String, Object> userFields,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    return toRepresentation(service.testAccess(spec).patch(id, userFields));
  }

  @DeleteMapping("/{id}")
  public void delete(
      @PathVariable("id") @NotNull final ID id,
      @Spec(path = "id", pathVars = "id", spec = Equal.class) @Parameter(hidden = true) SPEC spec) {
    service.testAccess(spec).deleteById(id);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<Object> collectionOptions() {
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

  protected abstract REPRESENTATION toRepresentation(MODEL entity);

  protected abstract Page<REPRESENTATION> toRepresentation(Page<MODEL> page);

  /**
   * A {@link AbstractAccessAwareController} using the model type M as output type and Page< M > as
   * the list output type.
   *
   * @param <M> The {@link AbstractBaseModel} type that is served by this controller.
   * @param <I> The input type used for POST and PUT methods.
   * @param <S> The {@link Specification} type used for filtering entities.
   */
  public abstract static class Default<
          M extends AbstractBaseModel<ID>,
          ID extends Serializable,
          I extends Identifiable<ID>,
          S extends Specification<M>>
      extends AbstractAccessAwareController<M, ID, I, M, S> {
    public Default(AbstractEntityService<M, ID, I> service) {
      super(service);
    }

    @Override
    protected M toRepresentation(M entity) {
      return entity;
    }

    @Override
    protected Page<M> toRepresentation(Page<M> page) {
      return page;
    }
  }
}
