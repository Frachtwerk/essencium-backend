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

package de.frachtwerk.essencium.backend.model.representation.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenRepresentation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiTokenAssemblerTest {

  @Mock UserServiceStub userServiceMock;

  @InjectMocks ApiTokenAssembler assembler;

  private ApiToken apiToken;
  private UserStub userStub;
  private Right right1;
  private Right right2;

  @BeforeEach
  void setUp() {
    userStub =
        UserStub.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();

    right1 = Right.builder().authority("READ").description("Read permission").build();
    right2 = Right.builder().authority("WRITE").description("Write permission").build();

    apiToken =
        ApiToken.builder()
            .id(UUID.randomUUID())
            .linkedUser("john.doe@example.com")
            .description("Test API Token")
            .validUntil(LocalDate.of(2025, 12, 31))
            .rights(Set.of(right1, right2))
            .token("test-token-123")
            .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
            .createdBy("admin")
            .updatedAt(LocalDateTime.of(2025, 1, 2, 11, 0))
            .updatedBy("admin")
            .build();
  }

  @Test
  void testToModel_shouldMapAllFields() {
    when(userServiceMock.loadUserByUsername("john.doe@example.com")).thenReturn(userStub);

    ApiTokenRepresentation result = assembler.toModel(apiToken);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(apiToken.getId());
    assertThat(result.getDescription()).isEqualTo("Test API Token");
    assertThat(result.getValidUntil()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(result.getToken()).isEqualTo("test-token-123");
    assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
    assertThat(result.getCreatedBy()).isEqualTo("admin");
    assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 2, 11, 0));
    assertThat(result.getUpdatedBy()).isEqualTo("admin");
    verify(userServiceMock, times(1)).loadUserByUsername("john.doe@example.com");
  }

  @Test
  void testToModel_shouldMapRights() {
    when(userServiceMock.loadUserByUsername("john.doe@example.com")).thenReturn(userStub);

    ApiTokenRepresentation result = assembler.toModel(apiToken);

    assertThat(result.getRights()).isNotNull();
    assertThat(result.getRights()).hasSize(2);
    assertThat(result.getRights()).containsExactlyInAnyOrder(right1, right2);
    verify(userServiceMock).loadUserByUsername("john.doe@example.com");
  }

  @Test
  void testToModel_shouldMapLinkedUser() {
    when(userServiceMock.loadUserByUsername("john.doe@example.com")).thenReturn(userStub);

    ApiTokenRepresentation result = assembler.toModel(apiToken);

    assertThat(result.getLinkedUser()).isNotNull();
    assertThat(result.getLinkedUser().id()).isEqualTo(1L);
    assertThat(result.getLinkedUser().name()).isEqualTo("John Doe");
    verify(userServiceMock).loadUserByUsername("john.doe@example.com");
  }

  @Test
  void testToModel_withNullOptionalFields() {
    ApiToken tokenWithNulls =
        ApiToken.builder()
            .id(UUID.randomUUID())
            .linkedUser("jane.doe@example.com")
            .description(null)
            .validUntil(null)
            .token(null)
            .build();
    UserStub janeUser =
        UserStub.builder()
            .id(2L)
            .firstName("Jane")
            .lastName("Doe")
            .email("jane.doe@example.com")
            .build();
    when(userServiceMock.loadUserByUsername("jane.doe@example.com")).thenReturn(janeUser);

    ApiTokenRepresentation result = assembler.toModel(tokenWithNulls);

    assertThat(result).isNotNull();
    assertThat(result.getDescription()).isNull();
    assertThat(result.getValidUntil()).isNull();
    assertThat(result.getToken()).isNull();
    assertThat(result.getLinkedUser()).isNotNull();
    assertThat(result.getLinkedUser().id()).isEqualTo(2L);
    verify(userServiceMock).loadUserByUsername("jane.doe@example.com");
  }

  @Test
  void testToModel_withEmptyRights() {
    ApiToken tokenWithoutRights =
        ApiToken.builder()
            .id(UUID.randomUUID())
            .linkedUser("john.doe@example.com")
            .description("Token without rights")
            .rights(Set.of())
            .build();
    when(userServiceMock.loadUserByUsername("john.doe@example.com")).thenReturn(userStub);

    ApiTokenRepresentation result = assembler.toModel(tokenWithoutRights);

    assertThat(result).isNotNull();
    assertThat(result.getRights()).isNotNull();
    assertThat(result.getRights()).isEmpty();
    verify(userServiceMock).loadUserByUsername("john.doe@example.com");
  }

  @Test
  void testToModel_shouldCallUserServiceWithCorrectUsername() {
    String expectedUsername = "test.user@example.com";
    ApiToken token =
        ApiToken.builder()
            .id(UUID.randomUUID())
            .linkedUser(expectedUsername)
            .description("Test token")
            .build();
    UserStub testUser =
        UserStub.builder()
            .id(3L)
            .firstName("Test")
            .lastName("User")
            .email(expectedUsername)
            .build();
    when(userServiceMock.loadUserByUsername(expectedUsername)).thenReturn(testUser);

    assembler.toModel(token);

    verify(userServiceMock).loadUserByUsername(expectedUsername);
    verifyNoMoreInteractions(userServiceMock);
  }
}
