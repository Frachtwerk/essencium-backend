package de.frachtwerk.essencium.backend.api.data.authentication;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class TestObjectsAuthentication {

  public UsernamePasswordAuthenticationToken defaultLoggedInPrincipal() {
    return new UsernamePasswordAuthenticationToken(TestObjects.users().internal(), null);
  }

  public UsernamePasswordAuthenticationToken notLoggedInPrincipal() {
    return new UsernamePasswordAuthenticationToken(null, null);
  }

  public UsernamePasswordAuthenticationToken externalLoggedInPrincipal() {
    return new UsernamePasswordAuthenticationToken(TestObjects.users().external(), null);
  }
}
