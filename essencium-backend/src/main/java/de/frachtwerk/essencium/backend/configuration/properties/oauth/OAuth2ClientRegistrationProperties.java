package de.frachtwerk.essencium.backend.configuration.properties.oauth;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.security.oauth2.client")
public class OAuth2ClientRegistrationProperties {

  private Map<String, Registration> registration;
  private Map<String, ClientProvider> provider;

  @EqualsAndHashCode
  @Data
  @Builder
  public static class Registration {
    private String provider;
    private String clientId;
    private String clientSecret;
    private String clientAuthenticationMethod;
    private String authorizationGrantType;
    private String redirectUri;
    private Set<String> scope;
    private String clientName;
    private String imageUrl;
    private ClientRegistrationAttributes attributes;
  }

  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientProvider {
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String userInfoAuthenticationMethod;
    private String userNameAttribute;
    private String jwkSetUri;
    private String issuerUri;
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
