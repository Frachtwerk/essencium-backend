package de.frachtwerk.essencium.backend.model.dto;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
public class RoleGrantedAuthority implements GrantedAuthority {
  private final String role;

  @Override
  public String getAuthority() {
    return role;
  }
}
