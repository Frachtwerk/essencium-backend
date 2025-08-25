package de.frachtwerk.essencium.backend.model.dto;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
public class RightGrantedAuthority implements GrantedAuthority {
  private final String role;

  @Override
  public String getAuthority() {
    return role;
  }
}
