package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.SessionTokenType;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class TokenRepresentation {

  private UUID id;

  private SessionTokenType type;

  private Date issuedAt;

  private Date expiration;

  private String userAgent;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastUsed;
}
