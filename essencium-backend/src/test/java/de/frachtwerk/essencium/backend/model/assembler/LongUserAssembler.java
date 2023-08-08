package de.frachtwerk.essencium.backend.model.assembler;

import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class LongUserAssembler extends AbstractRepresentationAssembler<TestLongUser, TestLongUser> {
  @Override
  public @NonNull TestLongUser toModel(@NonNull TestLongUser entity) {
    return entity;
  }
}
