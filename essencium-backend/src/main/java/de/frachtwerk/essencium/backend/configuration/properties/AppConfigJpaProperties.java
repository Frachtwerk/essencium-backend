package de.frachtwerk.essencium.backend.configuration.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.jpa")
public class AppConfigJpaProperties {
  private boolean camelCaseToUnderscore = false;
  private String tablePrefix = "FW_";
}
