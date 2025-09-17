package de.frachtwerk.essencium.backend.model.dto;

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuperBuilder
@Data
@Getter
@AllArgsConstructor
public class EssenciumUserDetails<ID extends Serializable> implements UserDetails {
  private final ID id;
  private final String username;
  private final String firstName;
  private final String lastName;
  private final String locale;
  private final Set<? extends GrantedAuthority> roles;
  private final Set<? extends GrantedAuthority> rights;
  private final Map<String, Object> additionalClaims;

  public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Stream.concat(
            roles.stream().map(role -> new RoleGrantedAuthority(role.getAuthority())),
            rights.stream().map(right -> new RightGrantedAuthority(right.getAuthority())))
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    return "";
  }

  public Locale getLocale() {
    return Objects.requireNonNullElse(Locale.forLanguageTag(locale), DEFAULT_LOCALE);
  }

  public Object getAdditionalClaimByKey(String key) {
    return additionalClaims.get(key);
  }
}
