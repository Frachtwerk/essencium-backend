package de.frachtwerk.essencium.backend.model.dto;

import org.springframework.security.core.GrantedAuthority;

public record RightGrantedAuthority(String right) implements GrantedAuthority {
  @Override
  public String getAuthority() {
    return right;
  }
}
