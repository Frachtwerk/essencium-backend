package de.frachtwerk.essencium.backend.model.dto;

import de.frachtwerk.essencium.backend.model.EssenciumUserDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;

@SuperBuilder
@Data
@Getter
public class EssenciumUserDetailsImpl<ID extends Serializable> implements EssenciumUserDetails<ID> {
  private final ID id;
  private final String username;
  private final String firstName;
  private final String lastName;
  private final String locale;
  private final Set<RoleGrantedAuthority> roles;
  private final Set<RightGrantedAuthority> rights;
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

  @Override
  public Locale getLocale() {
    return Objects.requireNonNullElse(Locale.forLanguageTag(locale), DEFAULT_LOCALE);
  }

  @Override
  public Map<String, Object> getAdditionalClaims() {
    return additionalClaims;
  }

  @Override
  public Object getAdditionalClaimByKey(String key) {
    return additionalClaims.get(key);
  }

  @Override
  public ID getId() {
    return id;
  }
}
