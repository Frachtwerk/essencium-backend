package de.frachtwerk.essencium.backend.model;

import de.frachtwerk.essencium.backend.model.dto.JwtRoleRights;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public interface EssenciumUserDetails<ID extends Serializable> extends UserDetails {

  Set<Role> getRoles();

  Set<JwtRoleRights> getRolesWithRights();

  Set<Right> getRights();

  Set<Right> getRightsForRole(String role);

  String getFirstName();

  String getLastName();

  Set<GrantedAuthority> convertToAuthorites(Collection<? extends GrantedAuthority> authoritesList);
}
