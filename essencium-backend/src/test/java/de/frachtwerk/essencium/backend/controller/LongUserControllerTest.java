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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.api.data.service.UserServiceStub;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.assembler.LongUserAssembler;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetailsImpl;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class LongUserControllerTest {

  private final UserServiceStub userServiceMock = Mockito.mock(UserServiceStub.class);

  private final LongUserAssembler assembler = new LongUserAssembler();

  private final LongUserController testSubject = new LongUserController(userServiceMock, assembler);

  @Test
  @SuppressWarnings("unchecked")
  void findAll() {
    Pageable testPageable = Mockito.mock(Pageable.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);
    Page userPageMock = Mockito.mock(Page.class);

    Mockito.when(userServiceMock.getAllFiltered(testSpecification, testPageable))
        .thenReturn(userPageMock);
    Mockito.when(userPageMock.map(any())).thenReturn(userPageMock);

    assertThat(testSubject.findAll(testSpecification, testPageable)).isSameAs(userPageMock);

    Mockito.verify(userServiceMock).getAllFiltered(any(), any());
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void findById() {
    var testId = 42L;
    var userMock = Mockito.mock(UserStub.class);
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);

    Mockito.when(userServiceMock.getOne(testSpecification)).thenReturn(Optional.of(userMock));

    assertThat(testSubject.findById(testSpecification)).isSameAs(userMock);

    Mockito.verify(userServiceMock).getOne(testSpecification);
  }

  @Test
  void create() {
    final var newUserEmail = "user@example.com";
    var testCreationUser = Mockito.mock(UserDto.class);
    when(testCreationUser.getEmail()).thenReturn(newUserEmail);

    var createdUserMock = Mockito.mock(UserStub.class);

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
    var testCreationUser = Mockito.mock(UserDto.class);
    when(testCreationUser.getEmail()).thenReturn(newUserEmail);

    Mockito.when(userServiceMock.loadUserByUsername(anyString()))
        .thenReturn(Mockito.mock(UserStub.class));

    assertThrows(DuplicateResourceException.class, () -> testSubject.create(testCreationUser));

    Mockito.verify(userServiceMock).loadUserByUsername(newUserEmail);
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void updateObject() {
    var testId = 42L;
    var testUpdateUser = Mockito.mock(UserDto.class);
    var updatedUserMock = Mockito.mock(UserStub.class);
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
    var testId = 42L;
    var updatedUserMock = Mockito.mock(UserStub.class);
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
    var testId = 42L;
    var updatedUserMock = Mockito.mock(UserStub.class);
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
    var testId = 42L;

    Mockito.when(userServiceMock.testAccess(testSpecification)).thenReturn(userServiceMock);

    testSubject.delete(testId, testSpecification);
    Mockito.verify(userServiceMock).deleteById(testId);
  }

  @Test
  void terminate() {
    var testId = 42L;
    BaseUserSpec testSpecification = Mockito.mock(BaseUserSpec.class);

    UserStub userStubMock = mock(UserStub.class);
    when(userServiceMock.getById(testId)).thenReturn(userStubMock);
    when(userStubMock.getUsername()).thenReturn("user@example.com");

    testSubject.terminate(testId, testSpecification);
    verify(userServiceMock).getById(testId);
    verify(userServiceMock).terminate("user@example.com");
    verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void getCurrentLoggedInUser() {
    EssenciumUserDetailsImpl<Long> jwtUserMock = mock(EssenciumUserDetailsImpl.class);
    var persistedUserMock = mock(UserStub.class);

    when(jwtUserMock.getId()).thenReturn(1L);
    when(userServiceMock.getById(1L)).thenReturn(persistedUserMock);

    assertThat(testSubject.getMe(jwtUserMock)).isSameAs(persistedUserMock);

    verify(userServiceMock).getById(1L);
  }

  @Test
  void updateCurrentLoggedInUser() {
    UserDto updateUserMock = mock(UserDto.class);
    UserStub persistedUserMock = mock(UserStub.class);
    EssenciumUserDetailsImpl<Long> essenciumUserDetails = mock(EssenciumUserDetailsImpl.class);

    when(userServiceMock.selfUpdate(persistedUserMock, updateUserMock))
        .thenReturn(persistedUserMock);
    when(userServiceMock.getById(essenciumUserDetails.getId())).thenReturn(persistedUserMock);
    assertThat(testSubject.updateMe(essenciumUserDetails, updateUserMock))
        .isSameAs(persistedUserMock);
  }

  @Test
  void getMyTokens() {
    UserStub userMock = mock(UserStub.class);
    SessionToken mockedAccessToken = mock(SessionToken.class);
    EssenciumUserDetailsImpl essenciumUserDetails = mock(EssenciumUserDetailsImpl.class);

    SessionToken sessionToken =
        SessionToken.builder()
            .id(UUID.randomUUID())
            .key(Jwts.SIG.HS512.key().build())
            .username("test")
            .type(SessionTokenType.REFRESH)
            .issuedAt(Date.from(LocalDateTime.now().minusWeeks(1).toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(LocalDateTime.now().plusWeeks(1).toInstant(ZoneOffset.UTC)))
            .parentToken(null)
            .accessTokens(List.of(mockedAccessToken))
            .userAgent("test")
            .build();
    LocalDateTime lastUsed = LocalDateTime.now().minusHours(1);
    when(mockedAccessToken.getIssuedAt()).thenReturn(Date.from(lastUsed.toInstant(ZoneOffset.UTC)));
    when(userServiceMock.getTokens(essenciumUserDetails.getUsername()))
        .thenReturn(List.of(sessionToken));
    List<TokenRepresentation> myTokens = testSubject.getMyTokens(essenciumUserDetails);

    verify(userServiceMock, times(1)).getTokens(essenciumUserDetails.getUsername());
    verifyNoMoreInteractions(userServiceMock);

    assertThat(myTokens).hasSize(1);

    TokenRepresentation tokenRepresentation = myTokens.get(0);
    assertEquals(tokenRepresentation.getId(), sessionToken.getId());
    assertEquals(tokenRepresentation.getIssuedAt(), sessionToken.getIssuedAt());
    assertEquals(tokenRepresentation.getExpiration(), sessionToken.getExpiration());
    assertEquals(tokenRepresentation.getType(), sessionToken.getType());
    assertEquals(tokenRepresentation.getUserAgent(), sessionToken.getUserAgent());
    assertEquals(
        tokenRepresentation.getLastUsed().truncatedTo(ChronoUnit.SECONDS),
        lastUsed.truncatedTo(ChronoUnit.SECONDS));
  }

  @Test
  void deleteToken() {
    EssenciumUserDetailsImpl userMock = mock(EssenciumUserDetailsImpl.class);
    UUID tokenId = UUID.randomUUID();
    testSubject.deleteToken(userMock, tokenId);
    when(userMock.getUsername()).thenReturn(null);
    verify(userServiceMock, times(1)).deleteToken(userMock.getUsername(), tokenId);
    verifyNoMoreInteractions(userServiceMock);
  }
}
