package de.frachtwerk.essencium.backend.model;

import static org.junit.jupiter.api.Assertions.*;

import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class ApiTokenUserTest {

  private static ApiTokenUser getApiTokenUser() {
    return ApiTokenUser.builder()
        .id(UUID.randomUUID())
        .linkedUser("user@app.com")
        .description("description")
        .rights(
            new HashSet<>(
                List.of(
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_CREATE.name())
                        .description("TRANSLATION_CREATE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_UPDATE.name())
                        .description("TRANSLATION_UPDATE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_DELETE.name())
                        .description("TRANSLATION_DELETE")
                        .build(),
                    Right.builder()
                        .authority(BasicApplicationRight.TRANSLATION_READ.name())
                        .description("TRANSLATION_READ")
                        .build())))
        .createdAt(LocalDateTime.now().minusWeeks(1))
        .validUntil(LocalDate.now().plusWeeks(1))
        .disabled(false)
        .build();
  }

  @Test
  void getAuthorities() {
    ApiTokenUser apiTokenUser = getApiTokenUser();
    assertEquals(4, apiTokenUser.getAuthorities().size());
    assertTrue(
        apiTokenUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(s -> s.equals(BasicApplicationRight.TRANSLATION_CREATE.name())));
    assertTrue(
        apiTokenUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(s -> s.equals(BasicApplicationRight.TRANSLATION_UPDATE.name())));
    assertTrue(
        apiTokenUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(s -> s.equals(BasicApplicationRight.TRANSLATION_DELETE.name())));
    assertTrue(
        apiTokenUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(s -> s.equals(BasicApplicationRight.TRANSLATION_READ.name())));
  }

  @Test
  void getPassword() {
    assertNull(getApiTokenUser().getPassword());
  }

  @Test
  void getUsername() {
    ApiTokenUser apiTokenUser = getApiTokenUser();
    UUID id = apiTokenUser.getId();
    assertEquals("user@app.com" + ":" + id, apiTokenUser.getUsername());
  }

  @Test
  void isAccountNonExpired() {
    ApiTokenUser apiTokenUser = getApiTokenUser();
    assertTrue(apiTokenUser.isAccountNonExpired());
    apiTokenUser.setValidUntil(LocalDate.now().minusDays(1));
    assertFalse(apiTokenUser.isAccountNonExpired());
  }

  @Test
  void isAccountNonLocked() {
    ApiTokenUser apiTokenUser = getApiTokenUser();
    assertTrue(apiTokenUser.isAccountNonLocked());
    apiTokenUser.setDisabled(true);
    assertFalse(apiTokenUser.isAccountNonLocked());
  }

  @Test
  void isEnabled() {
    ApiTokenUser apiTokenUser = getApiTokenUser();
    assertTrue(apiTokenUser.isEnabled());
    apiTokenUser.setDisabled(true);
    assertFalse(apiTokenUser.isEnabled());
  }
}
