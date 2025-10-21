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
public class ApiTokenRepresentation {
  private UUID id;
  private LocalDateTime createdAt;
  private String createdBy;
  private LocalDateTime updatedAt;
  private String updatedBy;
  private BasicRepresentation linkedUser;
  private String description;
  private LocalDate validUntil;
  @Builder.Default private Set<Right> rights = new HashSet<>();

  private String token;
}
