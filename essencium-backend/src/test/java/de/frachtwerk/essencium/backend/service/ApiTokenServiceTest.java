/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppJwtProperties;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.ApiTokenStatus;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.representation.assembler.ApiTokenAssembler;
import de.frachtwerk.essencium.backend.repository.ApiTokenRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiTokenService Unit Tests")
class ApiTokenServiceTest {

  @Mock private ApiTokenRepository repository;
  @Mock private AppJwtProperties appJwtProperties;
  @Mock private ApiTokenAssembler apiTokenAssembler;
  @Mock private RightService rightService;
  @Mock private JwtTokenService jwtTokenService;
  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;

  private ApiTokenService apiTokenService;

  private static final String TEST_USERNAME = "testuser@example.com";
  private static final String TEST_DESCRIPTION = "Test API Token";
  private static final int DEFAULT_EXPIRATION = 2592000;
  private static final String GENERATED_JWT = "generated.jwt.token";

  @BeforeEach
  void setUp() {
    apiTokenService =
        new ApiTokenService(
            repository, appJwtProperties, apiTokenAssembler, rightService, jwtTokenService);
    SecurityContextHolder.setContext(securityContext);
  }

  private EssenciumUserDetails<UUID> createMockUserDetails(Set<String> rightAuthorities) {
    Set<Right> rights = new HashSet<>();
    rightAuthorities.forEach(
        auth -> {
          Right right = Right.builder().authority(auth).build();
          rights.add(right);
        });

    return EssenciumUserDetails.<UUID>builder()
        .id(UUID.randomUUID())
        .username(TEST_USERNAME)
        .firstName("Test")
        .lastName("User")
        .roles(new HashSet<>())
        .rights(rights)
        .additionalClaims(new HashMap<>())
        .build();
  }

  private ApiTokenDto createApiTokenDto(Set<String> rights, LocalDate validUntil) {
    return ApiTokenDto.builder()
        .description(TEST_DESCRIPTION)
        .rights(rights)
        .validUntil(validUntil)
        .build();
  }

  private void setupSecurityContext(EssenciumUserDetails<?> userDetails) {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userDetails);
  }

  @Nested
  @DisplayName("createPreProcessing")
  class CreatePreProcessing {

    @Test
    @DisplayName("should throw IllegalStateException when no user context exists")
    void shouldThrowWhenNoUserContext() {
      when(securityContext.getAuthentication()).thenReturn(null);
      Set<String> rights = Set.of("READ", "WRITE");
      ApiTokenDto dto = createApiTokenDto(rights, null);

      assertThatThrownBy(() -> apiTokenService.create(dto))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("API Token creation requires a user context");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when user lacks requested rights")
    void shouldThrowWhenUserLacksRights() {
      Set<String> userRights = Set.of("READ");
      Set<String> requestedRights = Set.of("READ", "WRITE", "DELETE");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(requestedRights, null);

      assertThatThrownBy(() -> apiTokenService.create(dto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("User does not have all rights requested for the API token");
    }

    @Test
    @DisplayName("should succeed when user has all requested rights")
    void shouldSucceedWhenUserHasAllRights() {
      Set<String> userRights = Set.of("READ", "WRITE", "DELETE");
      Set<String> requestedRights = Set.of("READ", "WRITE");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(requestedRights, LocalDate.now().plusDays(30));

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      requestedRights.forEach(
          right ->
              when(rightService.findByAuthority(right))
                  .thenReturn(Optional.of(Right.builder().authority(right).build())));

      UUID savedId = UUID.randomUUID();
      when(repository.save(any(ApiToken.class)))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      ApiToken result = apiTokenService.create(dto);

      assertThat(result).isNotNull();
      verify(repository).save(any(ApiToken.class));
    }

    @Test
    @DisplayName("should succeed when user has exactly the requested rights")
    void shouldSucceedWhenUserHasExactRights() {
      Set<String> rights = Set.of("READ", "WRITE");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(rights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(rights, null);

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      rights.forEach(
          right ->
              when(rightService.findByAuthority(right))
                  .thenReturn(Optional.of(Right.builder().authority(right).build())));

      UUID savedId = UUID.randomUUID();
      when(repository.save(any(ApiToken.class)))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      ApiToken result = apiTokenService.create(dto);

      assertThat(result).isNotNull();
      verify(repository).save(any(ApiToken.class));
    }
  }

  @Nested
  @DisplayName("convertDtoToEntity")
  class ConvertDtoToEntity {

    @Test
    @DisplayName("should throw UnsupportedOperationException when updating existing token")
    void shouldThrowWhenUpdatingExistingToken() {
      UUID tokenId = UUID.randomUUID();

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), null);

      assertThatThrownBy(() -> apiTokenService.update(tokenId, dto))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("API Token updates via PUT method are not supported");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when validUntil is in the past")
    void shouldThrowWhenValidUntilInPast() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      LocalDate pastDate = LocalDate.now().minusDays(1);
      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), pastDate);

      assertThatThrownBy(() -> apiTokenService.create(dto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("API Token valid until date cannot be in the past");
    }

    @Test
    @DisplayName("should use default expiration when validUntil is null")
    void shouldUseDefaultExpirationWhenValidUntilIsNull() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), null);

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));

      UUID savedId = UUID.randomUUID();
      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      apiTokenService.create(dto);

      ApiToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getValidUntil()).isNotNull();
      assertThat(savedToken.getValidUntil()).isAfterOrEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("should correctly set linkedUser from authenticated user")
    void shouldSetLinkedUserFromAuthenticatedUser() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), LocalDate.now().plusDays(30));

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));

      UUID savedId = UUID.randomUUID();
      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      apiTokenService.create(dto);

      ApiToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getLinkedUser()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("should correctly map rights from DTO")
    void shouldMapRightsFromDto() {
      Set<String> userRights = Set.of("READ", "WRITE", "DELETE");
      Set<String> requestedRights = Set.of("READ", "WRITE");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(requestedRights, LocalDate.now().plusDays(30));

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      requestedRights.forEach(
          right ->
              when(rightService.findByAuthority(right))
                  .thenReturn(Optional.of(Right.builder().authority(right).build())));

      UUID savedId = UUID.randomUUID();
      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      apiTokenService.create(dto);

      ApiToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getRights()).hasSize(2);
      assertThat(savedToken.getRights().stream().map(Right::getAuthority))
          .containsExactlyInAnyOrder("READ", "WRITE");
    }

    @Test
    @DisplayName("should handle empty rights set")
    void shouldHandleEmptyRightsSet() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(new HashSet<>(), LocalDate.now().plusDays(30));

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);

      UUID savedId = UUID.randomUUID();
      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      apiTokenService.create(dto);

      ApiToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getRights()).isEmpty();
    }
  }

  @Nested
  @DisplayName("createPostProcessing")
  class CreatePostProcessing {

    @Test
    @DisplayName("should generate JWT token with correct parameters")
    @SuppressWarnings("unchecked")
    void shouldGenerateJwtTokenWithCorrectParameters() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      LocalDate validUntil = LocalDate.now().plusDays(30);
      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), validUntil);

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));

      UUID savedId = UUID.randomUUID();
      when(repository.save(any(ApiToken.class)))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });

      ArgumentCaptor<EssenciumUserDetails<?>> userDetailsCaptor =
          ArgumentCaptor.forClass(EssenciumUserDetails.class);
      ArgumentCaptor<SessionTokenType> tokenTypeCaptor =
          ArgumentCaptor.forClass(SessionTokenType.class);
      when(jwtTokenService.createToken(
              userDetailsCaptor.capture(), tokenTypeCaptor.capture(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      ApiToken result = apiTokenService.create(dto);

      assertThat(result.getToken()).isEqualTo(GENERATED_JWT);
      assertThat(tokenTypeCaptor.getValue()).isEqualTo(SessionTokenType.API);
      assertThat(userDetailsCaptor.getValue().getFirstName()).isEqualTo("API-Token");
      assertThat(userDetailsCaptor.getValue().getLastName()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("should set token field on saved entity")
    void shouldSetTokenFieldOnSavedEntity() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), LocalDate.now().plusDays(30));

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));

      UUID savedId = UUID.randomUUID();
      when(repository.save(any(ApiToken.class)))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      ApiToken result = apiTokenService.create(dto);

      assertThat(result.getToken()).isNotNull();
      assertThat(result.getToken()).isEqualTo(GENERATED_JWT);
    }
  }

  @Nested
  @DisplayName("updatePreProcessing")
  class UpdatePreProcessing {

    @Test
    @DisplayName("should throw UnsupportedOperationException for PUT updates")
    void shouldThrowForPutUpdates() {
      UUID tokenId = UUID.randomUUID();
      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), null);

      assertThatThrownBy(() -> apiTokenService.update(tokenId, dto))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("API Token updates via PUT method are not supported");
    }
  }

  @Nested
  @DisplayName("patchPreProcessing")
  class PatchPreProcessing {

    @Test
    @DisplayName("should revoke active token when status is set to REVOKED")
    void shouldRevokeActiveToken() {
      UUID tokenId = UUID.randomUUID();
      ApiToken activeToken =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.ACTIVE)
              .validUntil(LocalDate.now().plusDays(30))
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(activeToken));
      doNothing().when(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "REVOKED");

      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      ApiToken result = apiTokenService.patch(tokenId, updates);

      assertThat(result.getStatus()).isEqualTo(ApiTokenStatus.REVOKED);
      assertThat(result.getValidUntil()).isEqualTo(LocalDate.now());
      verify(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(activeToken.getUsername());
    }

    @Test
    @DisplayName("should throw IllegalStateException when token is not ACTIVE")
    void shouldThrowWhenTokenNotActive() {
      UUID tokenId = UUID.randomUUID();
      ApiToken revokedToken =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.REVOKED)
              .validUntil(LocalDate.now().minusDays(1))
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(revokedToken));

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "REVOKED");

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(InvalidInputException.class)
          .hasMessageContaining("current API token status must be ACTIVE");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for non-REVOKED status")
    void shouldThrowForNonRevokedStatus() {
      UUID tokenId = UUID.randomUUID();

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "ACTIVE");

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(InvalidInputException.class)
          .hasMessageContaining("only REVOKED status updates are allowed");
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when token does not exist")
    void shouldThrowWhenTokenNotFound() {
      UUID tokenId = UUID.randomUUID();

      when(repository.findById(tokenId)).thenReturn(Optional.empty());

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "REVOKED");

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw UnsupportedOperationException for non-status updates")
    void shouldThrowForNonStatusUpdates() {
      UUID tokenId = UUID.randomUUID();

      Map<String, Object> updates = new HashMap<>();
      updates.put("description", "New Description");

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("API Token updates via PATCH method are not supported");
    }

    @Test
    @DisplayName("should throw UnsupportedOperationException when status is not a String")
    void shouldThrowWhenStatusNotString() {
      UUID tokenId = UUID.randomUUID();

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", 123);

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("API Token updates via PATCH method are not supported");
    }

    @Test
    @DisplayName("should delete JWT tokens when revoking")
    void shouldDeleteJwtTokensWhenRevoking() {
      UUID tokenId = UUID.randomUUID();
      String tokenUsername = TEST_USERNAME + "-api-token-" + tokenId;
      ApiToken activeToken =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.ACTIVE)
              .validUntil(LocalDate.now().plusDays(30))
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(activeToken));
      doNothing().when(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "REVOKED");

      apiTokenService.patch(tokenId, updates);

      verify(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(tokenUsername);
    }
  }

  @Nested
  @DisplayName("deletePreProcessing")
  class DeletePreProcessing {

    @Test
    @DisplayName("should delete JWT tokens before deleting API token")
    void shouldDeleteJwtTokensBeforeDelete() {
      UUID tokenId = UUID.randomUUID();
      String tokenUsername = TEST_USERNAME + "-api-token-" + tokenId;
      ApiToken token =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.ACTIVE)
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(token));
      doNothing().when(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());
      doNothing().when(repository).deleteById(tokenId);

      apiTokenService.deleteById(tokenId);

      verify(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(tokenUsername);
      verify(repository).deleteById(tokenId);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when token does not exist")
    void shouldThrowWhenTokenNotFound() {
      UUID tokenId = UUID.randomUUID();

      when(repository.findById(tokenId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> apiTokenService.deleteById(tokenId))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(jwtTokenService, never()).deleteAllByUsernameEqualsIgnoreCase(anyString());
    }

    @Test
    @DisplayName("should handle null token username gracefully")
    void shouldHandleNullTokenUsername() {
      UUID tokenId = UUID.randomUUID();
      ApiToken token =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.ACTIVE)
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(token));
      doNothing().when(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());
      doNothing().when(repository).deleteById(tokenId);

      apiTokenService.deleteById(tokenId);

      verify(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());
    }
  }

  @Nested
  @DisplayName("getAssembler")
  class GetAssembler {

    @Test
    @DisplayName("should return correct assembler")
    void shouldReturnCorrectAssembler() {
      assertThat(apiTokenService.getAssembler()).isEqualTo(apiTokenAssembler);
    }

    @Test
    @DisplayName("should not return null assembler")
    void shouldNotReturnNullAssembler() {
      assertThat(apiTokenService.getAssembler()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Edge Cases and Null Safety")
  class EdgeCasesAndNullSafety {

    @Test
    @DisplayName("should handle null authentication in security context")
    void shouldHandleNullAuthentication() {
      when(securityContext.getAuthentication()).thenReturn(null);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), null);

      assertThatThrownBy(() -> apiTokenService.create(dto))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("API Token creation requires a user context");
    }

    @Test
    @DisplayName("should handle authentication with null principal")
    void shouldHandleNullPrincipal() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(null);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), null);

      assertThatThrownBy(() -> apiTokenService.create(dto))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("API Token creation requires a user context");
    }

    @Test
    @DisplayName("should handle validUntil on boundary date (today)")
    void shouldHandleValidUntilOnBoundaryDate() {
      Set<String> userRights = Set.of("READ");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      LocalDate today = LocalDate.now();
      ApiTokenDto dto = createApiTokenDto(Set.of("READ"), today);

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));

      UUID savedId = UUID.randomUUID();
      when(repository.save(any(ApiToken.class)))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      ApiToken result = apiTokenService.create(dto);

      assertThat(result).isNotNull();
      assertThat(result.getValidUntil()).isEqualTo(today);
    }

    @Test
    @DisplayName("should handle right service returning empty Optional")
    void shouldHandleRightServiceReturningEmpty() {
      Set<String> userRights = Set.of("READ", "WRITE");
      EssenciumUserDetails<UUID> userDetails = createMockUserDetails(userRights);
      setupSecurityContext(userDetails);

      ApiTokenDto dto = createApiTokenDto(Set.of("READ", "WRITE"), null);

      when(appJwtProperties.getDefaultApiTokenExpiration()).thenReturn(DEFAULT_EXPIRATION);
      when(rightService.findByAuthority("READ"))
          .thenReturn(Optional.of(Right.builder().authority("READ").build()));
      when(rightService.findByAuthority("WRITE")).thenReturn(Optional.empty());

      UUID savedId = UUID.randomUUID();
      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(
              invocation -> {
                ApiToken token = invocation.getArgument(0);
                token.setId(savedId);
                return token;
              });
      when(jwtTokenService.createToken(any(), any(), any(), any(), any()))
          .thenReturn(GENERATED_JWT);

      apiTokenService.create(dto);

      ApiToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getRights()).hasSize(1);
      assertThat(savedToken.getRights().stream().map(Right::getAuthority)).containsExactly("READ");
    }

    @Test
    @DisplayName("should handle multiple patch attempts with different statuses")
    void shouldHandleMultiplePatchAttempts() {
      UUID tokenId = UUID.randomUUID();
      ApiToken activeToken =
          ApiToken.builder()
              .id(tokenId)
              .linkedUser(TEST_USERNAME)
              .description(TEST_DESCRIPTION)
              .status(ApiTokenStatus.ACTIVE)
              .validUntil(LocalDate.now().plusDays(30))
              .build();

      when(repository.findById(tokenId)).thenReturn(Optional.of(activeToken));
      doNothing().when(jwtTokenService).deleteAllByUsernameEqualsIgnoreCase(anyString());

      Map<String, Object> updates = new HashMap<>();
      updates.put("status", "REVOKED");

      ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);
      when(repository.save(tokenCaptor.capture()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      ApiToken result = apiTokenService.patch(tokenId, updates);
      assertThat(result.getStatus()).isEqualTo(ApiTokenStatus.REVOKED);

      when(repository.findById(tokenId)).thenReturn(Optional.of(result));

      assertThatThrownBy(() -> apiTokenService.patch(tokenId, updates))
          .isInstanceOf(InvalidInputException.class)
          .hasMessageContaining("current API token status must be ACTIVE");
    }
  }
}
