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

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public abstract class AbstractRestController<
    IN, ID extends Serializable, OUT extends AbstractBaseModel<ID>> {
  protected final AbstractEntityService<OUT, ID, IN> service;

  public AbstractRestController(AbstractEntityService<OUT, ID, IN> service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public OUT findById(@PathVariable("id") @NotNull final ID id) {
    return service.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OUT create(@Valid @RequestBody @NotNull final IN input) {
    return service.create(input);
  }

  @PutMapping(value = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  public OUT updateObject(
      @PathVariable("id") @NotNull final ID id, @Valid @RequestBody @NotNull final IN input) {
    return service.update(id, input);
  }

  @PatchMapping(value = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  public OUT patch(
      @PathVariable("id") final ID id, @NotNull @RequestBody final Map<String, Object> fields) {
    return service.patch(id, fields);
  }

  @DeleteMapping(value = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") @NotNull final ID id) {
    service.deleteById(id);
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
