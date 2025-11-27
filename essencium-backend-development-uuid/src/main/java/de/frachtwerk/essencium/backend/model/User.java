package de.frachtwerk.essencium.backend.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class User extends AbstractBaseUser<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private LocalDate dateOfBirth;

  @Override
  public String getTitle() {
    return "";
  }

  @Override
  public Map<String, Object> getAdditionalClaims() {
    Map<String, Object> additionalClaims = new HashMap<>(super.getAdditionalClaims());
    additionalClaims.put("dateOfBirth", dateOfBirth);
    return additionalClaims;
  }
}
