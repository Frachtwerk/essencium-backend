package de.frachtwerk.essencium.backend.model.assembler;

import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UUIDUserAssembler extends AbstractRepresentationAssembler<TestUUIDUser, TestUUIDUser> {
  @Override
  public @NonNull TestUUIDUser toModel(@NonNull TestUUIDUser entity) {
    return entity;
  }
}
