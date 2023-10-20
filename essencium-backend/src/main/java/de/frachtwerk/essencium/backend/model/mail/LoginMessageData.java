package de.frachtwerk.essencium.backend.model.mail;

import de.frachtwerk.essencium.backend.configuration.properties.MailConfigProperties;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginMessageData {
  MailConfigProperties.Branding mailBranding;
  String email;
  String subject;
  TokenRepresentation tokenRepresentation;
}
