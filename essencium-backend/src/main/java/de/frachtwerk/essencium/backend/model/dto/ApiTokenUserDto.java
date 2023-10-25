package de.frachtwerk.essencium.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTokenUserDto {

  private UUID id;

  @NotBlank private String description;

  @Builder.Default private Set<String> rights = new HashSet<>();

  @NotNull private LocalDate validUntil;
}
