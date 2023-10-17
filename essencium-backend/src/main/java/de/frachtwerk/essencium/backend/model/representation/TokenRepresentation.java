package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.SessionTokenType;
import java.util.Date;
import java.util.UUID;
import lombok.*;

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

  // Fingerprinting: Browser, IP, User-Agent, etc.
}
