package de.frachtwerk.essencium.backend.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.cors")
@Validated
@Getter
@Setter
public class AppCorsProperties {
  private boolean allow;
}
