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

package de.frachtwerk.essencium.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.service.UserServiceStubUUID;
import de.frachtwerk.essencium.backend.api.data.user.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.assembler.UUIDUserAssembler;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.UserRepresentationDefaultAssembler;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UUIDUserControllerTest {

  private final UserServiceStubUUID userServiceMock = Mockito.mock(UserServiceStubUUID.class);
  private final UserRepresentationDefaultAssembler userRepresentationDefaultAssembler =
      new UserRepresentationDefaultAssembler();
  private final UUIDUserAssembler assembler = new UUIDUserAssembler();
  private final UUIDUserController testSubject = new UUIDUserController(userServiceMock, assembler);

  @Test
  @SuppressWarnings("unchecked")
  void findAll() {
    var testPageable = Mockito.mock(Pageable.class);
    var testSpecification = Mockito.mock(BaseUserSpec.class);
    var userPageMock = Mockito.mock(Page.class);

    Mockito.when(userServiceMock.getAllFiltered(testSpecification, testPageable))
        .thenReturn(userPageMock);
    Mockito.when(userPageMock.map(any())).thenReturn(userPageMock);

    assertThat(testSubject.findAll(testSpecification, testPageable)).isSameAs(userPageMock);

    Mockito.verify(userServiceMock).getAllFiltered(any(), any());
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void findById() {
    var testId = UUID.randomUUID();
    var userMock = Mockito.mock(TestUUIDUser.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);

    Mockito.when(userServiceMock.getOne(testSpecification)).thenReturn(Optional.of(userMock));

    assertThat(testSubject.findById(testSpecification)).isSameAs(userMock);

    Mockito.verify(userServiceMock).getOne(testSpecification);
  }

  @Test
  void create() {
    final var newUserEmail = "user@example.com";
    var testCreationUser = Mockito.mock(BaseUserDto.class);
    when(testCreationUser.getEmail()).thenReturn(newUserEmail);

    var createdUserMock = Mockito.mock(TestUUIDUser.class);

    Mockito.when(userServiceMock.loadUserByUsername(anyString()))
        .thenThrow(new UsernameNotFoundException(""));
    Mockito.when(userServiceMock.create(testCreationUser)).thenReturn(createdUserMock);

    assertThat(testSubject.create(testCreationUser)).isSameAs(createdUserMock);

    Mockito.verify(userServiceMock).loadUserByUsername(newUserEmail);
    Mockito.verify(userServiceMock).create(testCreationUser);
  }

  @Test
  void createAlreadyExisting() {
    final var newUserEmail = "user@example.com";
    var testCreationUser = Mockito.mock(BaseUserDto.class);
    when(testCreationUser.getEmail()).thenReturn(newUserEmail);

    Mockito.when(userServiceMock.loadUserByUsername(anyString()))
        .thenReturn(Mockito.mock(TestUUIDUser.class));

    assertThrows(DuplicateResourceException.class, () -> testSubject.create(testCreationUser));

    Mockito.verify(userServiceMock).loadUserByUsername(newUserEmail);
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void updateObject() {
    var testId = UUID.randomUUID();
    var testUpdateUser = Mockito.mock(BaseUserDto.class);
    var updatedUserMock = Mockito.mock(TestUUIDUser.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);

    Mockito.when(userServiceMock.testAccess(testSpecification)).thenReturn(userServiceMock);
    Mockito.when(userServiceMock.update(testId, testUpdateUser)).thenReturn(updatedUserMock);

    assertThat(testSubject.update(testId, testUpdateUser, testSpecification))
        .isSameAs(updatedUserMock);

    Mockito.verify(userServiceMock).update(testId, testUpdateUser);
  }

  @Test
  @SuppressWarnings("unchecked")
  void update() {
    var testId = UUID.randomUUID();
    var updatedUserMock = Mockito.mock(TestUUIDUser.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);
    Map<String, Object> testUserMap = Map.of("firstName", "James");

    Mockito.when(userServiceMock.testAccess(testSpecification)).thenReturn(userServiceMock);
    Mockito.when(userServiceMock.patch(testId, testUserMap)).thenReturn(updatedUserMock);

    assertThat(testSubject.update(testId, testUserMap, testSpecification))
        .isSameAs(updatedUserMock);
    Mockito.verify(userServiceMock).patch(testId, testUserMap);
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateSkipProtectedField() {
    var testId = UUID.randomUUID();
    var updatedUserMock = Mockito.mock(TestUUIDUser.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);
    Map<String, Object> testUserMap = Map.of("firstName", "James");

    ArgumentCaptor<Map<String, Object>> updateMapCaptor = ArgumentCaptor.forClass(Map.class);

    Mockito.when(userServiceMock.testAccess(testSpecification)).thenReturn(userServiceMock);
    Mockito.when(userServiceMock.patch(eq(testId), updateMapCaptor.capture()))
        .thenReturn(updatedUserMock);

    assertThat(testSubject.update(testId, testUserMap, testSpecification))
        .isSameAs(updatedUserMock);
    assertThat(updateMapCaptor.getValue()).containsOnlyKeys("firstName");
    Mockito.verify(userServiceMock).patch(any(), anyMap());
  }

  @Test
  void delete() {
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);
    var testId = UUID.randomUUID();

    Mockito.when(userServiceMock.testAccess(testSpecification)).thenReturn(userServiceMock);

    testSubject.delete(testId, testSpecification);
    Mockito.verify(userServiceMock).deleteById(testId);
  }

  @Test
  void terminate() {
    var testId = UUID.randomUUID();
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);

    TestUUIDUser userStubMock = mock(TestUUIDUser.class);
    when(userServiceMock.getById(testId)).thenReturn(userStubMock);
    when(userStubMock.getUsername()).thenReturn("user@example.com");
    testSubject.terminate(testId, testSpecification);
    verify(userServiceMock).getById(testId);
    verify(userServiceMock).terminate("user@example.com");
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void getCurrentLoggedInUser() {
    EssenciumUserDetails<UUID> AUTHUSERMock = mock(EssenciumUserDetails.class);
    var persistedUserMock = mock(TestUUIDUser.class);
    UUID persistedUserId = UUID.randomUUID();

    when(AUTHUSERMock.getId()).thenReturn(persistedUserId);
    when(userServiceMock.getById(persistedUserId)).thenReturn(persistedUserMock);
    assertThat(testSubject.getMe(AUTHUSERMock)).isSameAs(persistedUserMock);

    verify(userServiceMock).getById(persistedUserId);
  }

  @Test
  void updateCurrentLoggedInUser() {
    var updateUserMock = mock(BaseUserDto.class);
    var persistedUserMock = mock(TestUUIDUser.class);
    EssenciumUserDetails<UUID> essenciumUserDetailsMock = mock(EssenciumUserDetails.class);

    when(userServiceMock.selfUpdate(persistedUserMock, updateUserMock))
        .thenReturn(persistedUserMock);
    when(userServiceMock.getById(essenciumUserDetailsMock.getId())).thenReturn(persistedUserMock);

    assertThat(testSubject.updateMe(essenciumUserDetailsMock, updateUserMock))
        .isSameAs(persistedUserMock);
  }

  @Nested
  class TokenAdministration {
    private static final String USERNAME = "testuser";

    @Test
    void findAllWithTokens_Empty() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      when(userServiceMock.getAllFiltered(baseUserSpec)).thenReturn(Collections.emptyList());

      Map<UUID, List<TokenRepresentation>> allWithTokens =
          testSubject.findAllWithTokens(baseUserSpec);
      assertThat(allWithTokens).isEmpty();

      verify(userServiceMock, times(1)).getAllFiltered(baseUserSpec);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void findAllWithTokens_OneUserNoTokens() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      when(userServiceMock.getAllFiltered(baseUserSpec)).thenReturn(List.of(userMock));
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getTokens(USERNAME)).thenReturn(Collections.emptyList());

      Map<UUID, List<TokenRepresentation>> allWithTokens =
          testSubject.findAllWithTokens(baseUserSpec);
      assertThat(allWithTokens).hasSize(1);
      assertThat(allWithTokens.get(userMock.getId())).isEmpty();

      verify(userServiceMock, times(1)).getAllFiltered(baseUserSpec);
      verify(userServiceMock, times(1)).getTokens(USERNAME);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void findAllWithTokens_OneUserOneToken() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      SessionToken sessionToken1 = SessionToken.builder().id(UUID.randomUUID()).build();
      when(userServiceMock.getAllFiltered(baseUserSpec)).thenReturn(List.of(userMock));
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getTokens(USERNAME)).thenReturn(List.of(sessionToken1));

      Map<UUID, List<TokenRepresentation>> allWithTokens =
          testSubject.findAllWithTokens(baseUserSpec);
      assertThat(allWithTokens).hasSize(1);
      assertThat(allWithTokens.get(userMock.getId())).hasSize(1);
      assertThat(allWithTokens.get(userMock.getId()).getFirst().getId())
          .isEqualTo(sessionToken1.getId());

      verify(userServiceMock, times(1)).getAllFiltered(baseUserSpec);
      verify(userServiceMock, times(1)).getTokens(USERNAME);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void findAllWithTokens_OneUserManyTokens() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      SessionToken sessionToken1 = SessionToken.builder().id(UUID.randomUUID()).build();
      SessionToken sessionToken2 = SessionToken.builder().id(UUID.randomUUID()).build();
      when(userServiceMock.getAllFiltered(baseUserSpec)).thenReturn(List.of(userMock));
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getTokens(USERNAME)).thenReturn(List.of(sessionToken1, sessionToken2));

      Map<UUID, List<TokenRepresentation>> allWithTokens =
          testSubject.findAllWithTokens(baseUserSpec);
      assertThat(allWithTokens).hasSize(1);
      assertThat(allWithTokens.get(userMock.getId())).hasSize(2);
      assertThat(allWithTokens.get(userMock.getId()).stream().map(TokenRepresentation::getId))
          .containsExactlyInAnyOrder(sessionToken1.getId(), sessionToken2.getId());

      verify(userServiceMock, times(1)).getAllFiltered(baseUserSpec);
      verify(userServiceMock, times(1)).getTokens(USERNAME);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void findAllWithTokens_ManyUserOneTokenEach() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock1 = mock(TestUUIDUser.class);
      TestUUIDUser userMock2 = mock(TestUUIDUser.class);
      SessionToken sessionToken1 = SessionToken.builder().id(UUID.randomUUID()).build();
      SessionToken sessionToken2 = SessionToken.builder().id(UUID.randomUUID()).build();
      when(userServiceMock.getAllFiltered(baseUserSpec)).thenReturn(List.of(userMock1, userMock2));
      when(userMock1.getId()).thenReturn(UUID.randomUUID());
      when(userMock2.getId()).thenReturn(UUID.randomUUID());
      when(userMock1.getUsername()).thenReturn(USERNAME + "1");
      when(userMock2.getUsername()).thenReturn(USERNAME + "2");
      when(userServiceMock.getTokens(USERNAME + "1")).thenReturn(List.of(sessionToken1));
      when(userServiceMock.getTokens(USERNAME + "2")).thenReturn(List.of(sessionToken2));

      Map<UUID, List<TokenRepresentation>> allWithTokens =
          testSubject.findAllWithTokens(baseUserSpec);
      assertThat(allWithTokens).hasSize(2);
      assertThat(allWithTokens.get(userMock1.getId())).hasSize(1);
      assertThat(allWithTokens.get(userMock1.getId()).stream().map(TokenRepresentation::getId))
          .containsExactlyInAnyOrder(sessionToken1.getId());
      assertThat(allWithTokens.get(userMock2.getId())).hasSize(1);
      assertThat(allWithTokens.get(userMock2.getId()).stream().map(TokenRepresentation::getId))
          .containsExactlyInAnyOrder(sessionToken2.getId());

      verify(userServiceMock, times(1)).getAllFiltered(baseUserSpec);
      verify(userServiceMock, times(1)).getTokens(USERNAME + "1");
      verify(userServiceMock, times(1)).getTokens(USERNAME + "2");
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getTokensByUserId_UserNotFound() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      UUID userId = UUID.randomUUID();
      when(userServiceMock.getById(userId)).thenThrow(ResourceNotFoundException.class);
      assertThrows(
          ResourceNotFoundException.class,
          () -> testSubject.getTokensByUserId(userId, baseUserSpec));

      verify(userServiceMock, times(1)).getById(userId);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getTokensByUserId_UserFoundNoTokens() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      UUID userId = UUID.randomUUID();
      when(userMock.getId()).thenReturn(userId);
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getById(userId)).thenReturn(userMock);
      Map<UUID, List<TokenRepresentation>> tokensByUserId =
          testSubject.getTokensByUserId(userId, baseUserSpec);
      assertThat(tokensByUserId).hasSize(1);
      assertThat(tokensByUserId.get(userId)).isEmpty();

      verify(userServiceMock, times(1)).getById(userId);
      verify(userServiceMock, times(1)).getTokens(USERNAME);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getTokensByUserId_UserFoundWithTokens() {
      BaseUserSpec<TestUUIDUser, UUID> baseUserSpec = mock(BaseUserSpec.class);
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      UUID userId = UUID.randomUUID();
      SessionToken sessionToken1 = SessionToken.builder().id(UUID.randomUUID()).build();
      when(userMock.getId()).thenReturn(userId);
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getById(userId)).thenReturn(userMock);
      when(userServiceMock.getTokens(USERNAME)).thenReturn(List.of(sessionToken1));
      Map<UUID, List<TokenRepresentation>> tokensByUserId =
          testSubject.getTokensByUserId(userId, baseUserSpec);
      assertThat(tokensByUserId).hasSize(1);
      assertThat(tokensByUserId.get(userId)).hasSize(1);
      assertThat(tokensByUserId.get(userId).getFirst().getId()).isEqualTo(sessionToken1.getId());

      verify(userServiceMock, times(1)).getById(userId);
      verify(userServiceMock, times(1)).getTokens(USERNAME);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void deleteTokenById_UserNotFound() {
      UUID userId = UUID.randomUUID();
      UUID tokenId = UUID.randomUUID();
      when(userServiceMock.getById(userId)).thenThrow(ResourceNotFoundException.class);
      assertThrows(
          ResourceNotFoundException.class, () -> testSubject.deleteTokenById(userId, tokenId));

      verify(userServiceMock, times(1)).getById(userId);
      verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void deleteTokenById_UserFound() {
      TestUUIDUser userMock = mock(TestUUIDUser.class);
      UUID userId = UUID.randomUUID();
      when(userMock.getId()).thenReturn(userId);
      when(userMock.getUsername()).thenReturn(USERNAME);
      when(userServiceMock.getById(userId)).thenReturn(userMock);
      testSubject.deleteTokenById(userId, UUID.randomUUID());

      verify(userServiceMock, times(1)).deleteToken(eq(USERNAME), any(UUID.class));

      verify(userServiceMock, times(1)).getById(userId);
      verifyNoMoreInteractions(userServiceMock);
    }
  }
}
