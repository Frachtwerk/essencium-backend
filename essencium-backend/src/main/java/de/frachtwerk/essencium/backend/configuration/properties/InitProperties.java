package de.frachtwerk.essencium.backend.configuration.properties;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.init")
public class InitProperties {
  private Set<UserProperties> users = new HashSet<>();
  private Set<RoleProperties> roles = new HashSet<>();

  public Set<RoleProperties> getRoles() {
    if (roles.stream().noneMatch(role -> role.getName().equals("ADMIN"))) {
      roles.add(new RoleProperties());
    }
    return roles;
  }
}
