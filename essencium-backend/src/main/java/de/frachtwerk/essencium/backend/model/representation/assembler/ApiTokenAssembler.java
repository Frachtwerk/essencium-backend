package de.frachtwerk.essencium.backend.model.representation.assembler;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.BasicRepresentation;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.io.Serializable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiTokenAssembler
    extends AbstractRepresentationAssembler<ApiToken, ApiTokenRepresentation> {

  private final AbstractUserService<
          ? extends AbstractBaseUser<?>,
          ? extends EssenciumUserDetails<?>,
          ? extends Serializable,
          ? extends BaseUserDto<?>>
      userService;

  @Override
  public @NonNull ApiTokenRepresentation toModel(@NonNull ApiToken entity) {
    return ApiTokenRepresentation.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedAt(entity.getUpdatedAt())
        .updatedBy(entity.getUpdatedBy())
        .linkedUser(
            BasicRepresentation.from(userService.loadUserByUsername(entity.getLinkedUser())))
        .description(entity.getDescription())
        .validUntil(entity.getValidUntil())
        .rights(entity.getRights())
        .token(entity.getToken())
        .build();
  }
}
