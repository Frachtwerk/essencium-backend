package de.frachtwerk.essencium.backend.model.dto;

import de.frachtwerk.essencium.backend.model.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record EssenciumUserDetailsImpl<ID extends Serializable>(
    ID id,
    String username,
    String firstName,
    String lastName,
    String locale,
    List<JwtRoleRights> rolesWithRights,
    Map<String, Object> additionalClaims)
    implements EssenciumUserDetails<ID> {

  public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return rolesWithRights.stream()
        .flatMap(
            r ->
                Stream.concat(
                    Stream.of(new SimpleGrantedAuthority(r.getRole())),
                    r.getRights().stream().map(SimpleGrantedAuthority::new)))
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public Set<Role> getRoles() {
    return rolesWithRights.stream()
        .map(
            r ->
                Role.builder()
                    .name(r.getRole())
                    .rights(
                        r.getRights().stream()
                            .map(right -> Right.builder().authority(right).build())
                            .collect(Collectors.toSet()))
                    .build())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<JwtRoleRights> getRolesWithRights() {
    return rolesWithRights.stream()
        .map(r -> new JwtRoleRights(r.getRole(), r.getRights()))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Right> getRights() {
    return rolesWithRights.stream()
        .flatMap(r -> r.getRights().stream())
        .map(right -> Right.builder().authority(right).build())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Right> getRightsForRole(String role) {
    return rolesWithRights.stream()
        .filter(r -> r.getRole().equals(role))
        .flatMap(r -> r.getRights().stream())
        .map(right -> Right.builder().authority(right).build())
        .collect(Collectors.toSet());
  }

  @Override
  public String getFirstName() {
    return firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public Set<GrantedAuthority> convertToAuthorites(
      Collection<? extends GrantedAuthority> authoritesList) {
    return authoritesList.stream()
        .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
        .collect(Collectors.toSet());
  }

  @Override
  public Locale getLocale() {
    return Objects.requireNonNullElse(Locale.forLanguageTag(locale), DEFAULT_LOCALE);
  }

  @Override
  public Map<String, Object> getAdditionalClaims() {
    return additionalClaims;
  }

  @Override
  public ID getId() {
    return id;
  }
}
