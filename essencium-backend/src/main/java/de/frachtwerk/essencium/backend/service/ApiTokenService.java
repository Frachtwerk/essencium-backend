package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.util.UserUtil.getRightsFromUserDetails;
import static de.frachtwerk.essencium.backend.util.UserUtil.getUserDetailsFromAuthentication;

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.model.representation.assembler.ApiTokenAssembler;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
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

    EssenciumUserDetails<? extends Serializable> essenciumUserDetails =
        getUserDetailsFromAuthentication()
            .orElseThrow(
                () -> new IllegalStateException("API Token creation requires a user context"));

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
    EssenciumUserDetails<? extends Serializable> userDetails =
        getUserDetailsFromAuthentication()
            .orElseThrow(
                () -> new IllegalStateException("API Token creation requires a user context"));
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
            Date.from(
                saved.getValidUntil().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())));
    return super.createPostProcessing(saved);
  }

  @Override
  public AbstractRepresentationAssembler<ApiToken, ApiTokenRepresentation> getAssembler() {
    return apiTokenAssembler;
  }
}
