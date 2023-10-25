package de.frachtwerk.essencium.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApiTokenUser implements UserDetails {

  @Id @UuidGenerator private UUID id;

  private String user;

  private String description;

  @OneToMany @Builder.Default private Set<Right> rights = new HashSet<>();

  @CreatedDate private LocalDateTime createdAt;

  private LocalDate validUntil;

  private boolean disabled;

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
    return user + ":" + id;
  }

  @Override
  public boolean isAccountNonExpired() {
    return (LocalDate.now().isBefore(validUntil) || LocalDate.now().isEqual(validUntil));
  }

  @Override
  public boolean isAccountNonLocked() {
    return !disabled;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return !disabled;
  }

  @Override
  public boolean isEnabled() {
    return isAccountNonExpired() && isAccountNonLocked();
  }
}
