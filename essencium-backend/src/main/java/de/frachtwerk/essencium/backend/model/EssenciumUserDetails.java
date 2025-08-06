package de.frachtwerk.essencium.backend.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public interface EssenciumUserDetails<ID extends Serializable> extends UserDetails {

  Set<? extends GrantedAuthority> getRoles();

  Set<? extends GrantedAuthority> getRights();

  String getFirstName();

  String getLastName();

  Locale getLocale();

  Map<String, Object> getAdditionalClaims();

  Object getAdditionalClaimByKey(String key);

  ID getId();
}
