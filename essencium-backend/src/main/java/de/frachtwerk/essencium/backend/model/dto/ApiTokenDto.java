package de.frachtwerk.essencium.backend.model.dto;

import de.frachtwerk.essencium.backend.model.Identifiable;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiTokenDto implements Identifiable<UUID> {
  private UUID id;
  @NotBlank private String description;
  private LocalDate validUntil;
  private Set<String> rights = new HashSet<>();
}
