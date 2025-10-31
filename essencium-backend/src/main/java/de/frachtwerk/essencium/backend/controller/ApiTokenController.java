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

import de.frachtwerk.essencium.backend.controller.access.ExposesEntity;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenDto;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.ApiTokenSpecification;
import de.frachtwerk.essencium.backend.service.ApiTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api-tokens")
@ExposesEntity(ApiToken.class)
@Tag(name = "ApiTokenController", description = "CRUD operations for API tokens")
public class ApiTokenController
    extends AbstractAccessAwareController<
        ApiToken, UUID, ApiTokenDto, ApiTokenRepresentation, ApiTokenSpecification> {

  public ApiTokenController(ApiTokenService service) {
    super(service);
  }

  @Override
  protected ApiTokenRepresentation toRepresentation(ApiToken entity) {
    return ((ApiTokenService) service).getAssembler().toModel(entity);
  }

  @Override
  protected Page<ApiTokenRepresentation> toRepresentation(Page<ApiToken> page) {
    return page.map(this::toRepresentation);
  }
}
