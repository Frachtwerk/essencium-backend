package de.frachtwerk.essencium.backend.model.assembler;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class LongUserAssembler extends AbstractRepresentationAssembler<UserStub, UserStub> {
  @Override
  public @NonNull UserStub toModel(@NonNull UserStub entity) {
    return entity;
  }
}
