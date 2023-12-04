package de.frachtwerk.essencium.backend.configuration.properties.oauth;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.security.oauth2.client")
public class OAuth2ClientRegistrationProperties {

  private Map<String, Registration> registration;
  private Map<String, ClientProvider> provider;

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class Registration extends OAuth2ClientProperties.Registration {
    private String clientName;
    private String imageUrl;
    private ClientRegistrationAttributes attributes;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class ClientProvider extends OAuth2ClientProperties.Provider {
    private ClientRegistrationAttributes clientRegistrationAttributes;
  }

  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientRegistrationAttributes {
    private String username;
    private String firstname;
    private String lastname;
    private String name;
  }
}
