package de.frachtwerk.essencium.backend.util;

import de.frachtwerk.essencium.backend.model.validation.ValidEmail;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestEmail {
  @ValidEmail private String email;
}
