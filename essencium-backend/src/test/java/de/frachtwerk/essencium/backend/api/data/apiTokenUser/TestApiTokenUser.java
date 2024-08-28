package de.frachtwerk.essencium.backend.api.data.apiTokenUser;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
}
