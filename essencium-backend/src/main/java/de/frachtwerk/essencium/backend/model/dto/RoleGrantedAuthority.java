package de.frachtwerk.essencium.backend.model.dto;

import org.springframework.security.core.GrantedAuthority;

public record RoleGrantedAuthority(String role) implements GrantedAuthority {
  @Override
  public String getAuthority() {
    return role;
  }
}
