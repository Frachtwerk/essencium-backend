package de.frachtwerk.essencium.backend.model.dto;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
  @NotBlank private String name;

  private String description;

  private boolean isProtected;

  private boolean isDefaultRole;

  @Builder.Default private Set<Object> rights = new HashSet<>();

  public Role toRole() {
    HashSet<Right> convertedRights = new HashSet<>();
    if (!rights.isEmpty()) {
      // patterns in switch statements are a preview feature and are disabled by default.
      if (rights.iterator().next() instanceof Right) {
        convertedRights.addAll(
            rights.stream().map(o -> (Right) o).collect(Collectors.toCollection(HashSet::new)));
      } else if (rights.iterator().next() instanceof Map<?, ?>) {
        convertedRights.addAll(
            rights.stream()
                .map(o -> (Map<String, String>) o)
                .map(
                    map ->
                        Right.builder()
                            .authority(map.get("authority"))
                            .description(map.get("description"))
                            .build())
                .collect(Collectors.toCollection(HashSet::new)));
      } else if (rights.iterator().next() instanceof String) {
        convertedRights.addAll(
            rights.stream()
                .map(o -> Right.builder().authority((String) o).build())
                .collect(Collectors.toCollection(HashSet::new)));
      }
    }
    return Role.builder()
        .name(name)
        .description(description)
        .isProtected(isProtected)
        .isDefaultRole(isDefaultRole)
        .rights(convertedRights)
        .build();
  }
}
