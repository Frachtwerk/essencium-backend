package de.frachtwerk.essencium.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"linked_user", "description"})})
public class ApiToken extends UUIDModel implements UserDetails {

  /** username/email of the user this token is linked to */
  @NotNull
  @Column(name = "linked_user")
  private String linkedUser;

  @Column(name = "description")
  private String description;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @ManyToMany(fetch = FetchType.EAGER)
  @Builder.Default
  private Set<Right> rights = new HashSet<>();

  @Transient private String token;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return rights;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return linkedUser + "-api-token-" + id;
  }

  @Override
  public boolean isAccountNonExpired() {
    return validUntil == null || validUntil.isAfter(LocalDate.now());
  }

  @Override
  public boolean isAccountNonLocked() {
    return isAccountNonExpired();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return isAccountNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return isAccountNonExpired();
  }

  @Override
  public String getTitle() {
    return getUsername();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiToken apiToken)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(getId(), apiToken.getId())
        && Objects.equals(getLinkedUser(), apiToken.getLinkedUser())
        && Objects.equals(getDescription(), apiToken.getDescription());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getLinkedUser(), getDescription());
  }
}
