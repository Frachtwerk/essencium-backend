package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.Right;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ApiTokenUserRepresentation {
  private UUID id;
  private String linkedUser;
  private String description;
  @Builder.Default private Set<Right> rights = new HashSet<>();
  private LocalDateTime createdAt;
  private LocalDate validUntil;
  private boolean disabled;

  // This field is not present in the original model. It is only filled on creation of a new token.
  private String token;
}
