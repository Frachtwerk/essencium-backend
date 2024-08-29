package de.frachtwerk.essencium.backend.api.data.apiTokenUser;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenUserDto;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestApiTokenUser {

  public ApiTokenUser defaultUser() {
    return ApiTokenUser.builder()
        .id(UUID.randomUUID())
        .linkedUser("user@app.com")
        .description("description")
        .rights(
            new HashSet<>(
                List.of(
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                        .description("TRANSLATION_CREATE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                        .description("TRANSLATION_UPDATE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                        .description("TRANSLATION_DELETE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_READ.name())
                        .description("TRANSLATION_READ")
                        .build())))
        .createdAt(LocalDateTime.now().minusWeeks(1))
        .validUntil(LocalDate.now().plusWeeks(1))
        .disabled(false)
        .build();
  }

  public ApiTokenUser defaultUserWithRights(List<Right> rights) {
    return ApiTokenUser.builder()
        .id(UUID.randomUUID())
        .linkedUser("user@app.com")
        .description("description")
        .rights(new HashSet<>(rights))
        .createdAt(LocalDateTime.now().minusWeeks(1))
        .validUntil(LocalDate.now().plusWeeks(1))
        .disabled(false)
        .build();
  }

  public ApiTokenUserDto defaultUserDto() {
    return ApiTokenUserDto.builder()
        .description("description")
        .rights(
            Set.of(
                BasicApplicationRight.TRANSLATION_CREATE.name(),
                BasicApplicationRight.TRANSLATION_UPDATE.name(),
                BasicApplicationRight.TRANSLATION_DELETE.name(),
                BasicApplicationRight.TRANSLATION_READ.name()))
        .build();
  }
}
