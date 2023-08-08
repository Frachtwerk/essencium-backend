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

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rights")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.right-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(
    name = "RightController",
    description = "Set of endpoints to manage global application rights / permissions")
public class RightController {
  private final RightService rightService;

  public RightController(final RightService rightService) {
    this.rightService = rightService;
  }

  @GetMapping
  @Secured({BasicApplicationRight.Authority.RIGHT_READ})
  @Operation(description = "List all available rights")
  public Page<Right> findAll(@NotNull final Pageable pageable) {
    return rightService.getAll(pageable);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }
}
