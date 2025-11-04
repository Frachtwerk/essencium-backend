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

package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.util.UserUtil.getRightsFromUserDetails;
import static de.frachtwerk.essencium.backend.util.UserUtil.getUserDetailsFromAuthentication;

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.model.representation.assembler.ApiTokenAssembler;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ApiTokenService extends AbstractEntityService<ApiToken, UUID, ApiTokenDto>
    implements AssemblingService<ApiToken, ApiTokenRepresentation> {

  private final ApiTokenAssembler apiTokenAssembler;
  private final RightService rightService;
  private final JwtTokenService jwtTokenService;

  protected ApiTokenService(
      ApiTokenRepository repository,
      ApiTokenAssembler apiTokenAssembler,
      RightService rightService,
      JwtTokenService jwtTokenService) {
    super(repository);
    this.apiTokenAssembler = apiTokenAssembler;
    this.rightService = rightService;
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected <E extends ApiTokenDto> ApiToken createPreProcessing(E dto) {
    EssenciumUserDetails<?> userDetails =
        getUserDetailsFromAuthentication()
            .orElseThrow(
                () -> new IllegalStateException("API Token creation requires a user context"));

    HashSet<String> rights = getRightsFromUserDetails(userDetails);

    if (!rights.containsAll(dto.getRights())) {
      throw new IllegalArgumentException(
          "User does not have all rights requested for the API token");
    }

    return super.createPreProcessing(dto);
  }

  @Override
  protected <E extends ApiTokenDto> ApiToken convertDtoToEntity(
      E dto, Optional<ApiToken> currentEntityOpt) {
    if (currentEntityOpt.isPresent()) {
      throw new IllegalStateException("API Token cannot be updated");
    }

    if (Objects.nonNull(dto.getValidUntil()) && dto.getValidUntil().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("API Token valid until date cannot be in the past");
    }

    Optional<EssenciumUserDetails<? extends Serializable>> userDetailsOptional =
        getUserDetailsFromAuthentication();
    assert userDetailsOptional.isPresent(); // already checked in createPreProcessing
    EssenciumUserDetails<? extends Serializable> essenciumUserDetails = userDetailsOptional.get();

    return ApiToken.builder()
        .linkedUser(essenciumUserDetails.getUsername())
        .description(dto.getDescription())
        .validUntil(dto.getValidUntil())
        .rights(
            dto.getRights().stream()
                .flatMap(s -> rightService.findByAuthority(s).stream())
                .collect(Collectors.toSet()))
        .build();
  }

  @Override
  protected ApiToken createPostProcessing(ApiToken saved) {
    Optional<EssenciumUserDetails<? extends Serializable>> userDetailsOptional =
        getUserDetailsFromAuthentication();
    assert userDetailsOptional.isPresent();
    EssenciumUserDetails<? extends Serializable> userDetails = userDetailsOptional.get();

    EssenciumUserDetails<? extends Serializable> tokenUserDetails =
        EssenciumUserDetails.builder()
            .id(saved.getId())
            .username(saved.getUsername())
            .firstName("API-Token")
            .lastName(saved.getLinkedUser())
            .roles(new HashSet<>())
            .rights(saved.getRights())
            .additionalClaims(userDetails.getAdditionalClaims())
            .build();
    saved.setToken(
        jwtTokenService.createToken(
            tokenUserDetails,
            SessionTokenType.API,
            null,
            null,
            Optional.ofNullable(saved.getValidUntil())
                .map(
                    localDate ->
                        localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                .map(Date::from)
                .orElse(null)));
    return super.createPostProcessing(saved);
  }

  @Override
  protected void deletePreProcessing(UUID uuid) {
    ApiToken apiToken = repository.findById(uuid).orElseThrow(ResourceNotFoundException::new);
    jwtTokenService.deleteAllbyUsernameEqualsIgnoreCase(apiToken.getUsername());
  }

  @Override
  public AbstractRepresentationAssembler<ApiToken, ApiTokenRepresentation> getAssembler() {
    return apiTokenAssembler;
  }
}
