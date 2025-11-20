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

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.ApiTokenStatus;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiTokenService extends AbstractEntityService<ApiToken, UUID, ApiTokenDto>
    implements AssemblingService<ApiToken, ApiTokenRepresentation> {

  private final AppJwtProperties appJwtProperties;
  private final ApiTokenAssembler apiTokenAssembler;
  private final RightService rightService;
  private final JwtTokenService jwtTokenService;

  @Autowired
  protected ApiTokenService(
      ApiTokenRepository repository,
      AppJwtProperties appJwtProperties,
      ApiTokenAssembler apiTokenAssembler,
      RightService rightService,
      JwtTokenService jwtTokenService) {
    super(repository);
    this.appJwtProperties = appJwtProperties;
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
        .validUntil(
            Objects.requireNonNullElse(
                dto.getValidUntil(),
                LocalDateTime.now()
                    .plusSeconds(appJwtProperties.getDefaultApiTokenExpiration())
                    .toLocalDate()))
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
            .additionalClaims(
                userDetails.getAdditionalClaims().entrySet().stream()
                    .filter(entry -> !JwtTokenService.getDefaultClaims().contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
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
  protected <E extends ApiTokenDto> ApiToken updatePreProcessing(UUID uuid, E dto) {
    // PUT-Updates are not allowed for API tokens
    throw new UnsupportedOperationException("API Token updates via PUT method are not supported");
  }

  @Override
  protected ApiToken patchPreProcessing(UUID uuid, Map<String, Object> fieldUpdates) {
    // Only status updates are allowed via PATCH
    if (fieldUpdates.containsKey("status")) {
      Object object = fieldUpdates.get("status");
      if (object instanceof String string) {
        ApiTokenStatus status = ApiTokenStatus.valueOf(string);
        if (!Objects.equals(ApiTokenStatus.REVOKED, status)) {
          throw new IllegalArgumentException("only REVOKED status updates are allowed");
        }
        ApiToken apiToken = repository.findById(uuid).orElseThrow(ResourceNotFoundException::new);
        if (!Objects.equals(apiToken.getStatus(), ApiTokenStatus.ACTIVE)) {
          throw new IllegalStateException("current API token status must be ACTIVE");
        }
        jwtTokenService.deleteAllByUsernameEqualsIgnoreCase(apiToken.getUsername());
        apiToken.setStatus(status);
        apiToken.setValidUntil(LocalDate.now());
        return apiToken;
      }
    }
    throw new UnsupportedOperationException("API Token updates via PATCH method are not supported");
  }

  @Override
  protected void deletePreProcessing(UUID uuid) {
    ApiToken apiToken = repository.findById(uuid).orElseThrow(ResourceNotFoundException::new);
    jwtTokenService.deleteAllByUsernameEqualsIgnoreCase(apiToken.getUsername());
  }

  @Override
  public AbstractRepresentationAssembler<ApiToken, ApiTokenRepresentation> getAssembler() {
    return apiTokenAssembler;
  }
}
