/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.assembler.LongUserAssembler;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.service.LongUserService;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class LongUserControllerTest {

  private final LongUserService userServiceMock = Mockito.mock(LongUserService.class);

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
    var userMock = Mockito.mock(TestLongUser.class);

    Mockito.when(userServiceMock.getById(testId)).thenReturn(userMock);

    assertThat(testSubject.findById(testId)).isSameAs(userMock);

    Mockito.verify(userServiceMock).getById(testId);
  }

  @Test
  void create() {
    final var newUserEmail = "user@example.com";
    var testCreationUser = Mockito.mock(UserDto.class);
    when(testCreationUser.getEmail()).thenReturn(newUserEmail);

    var createdUserMock = Mockito.mock(TestLongUser.class);

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
        .thenReturn(Mockito.mock(TestLongUser.class));

    assertThrows(DuplicateResourceException.class, () -> testSubject.create(testCreationUser));

    Mockito.verify(userServiceMock).loadUserByUsername(newUserEmail);
    Mockito.verifyNoMoreInteractions(userServiceMock);
  }

  @Test
  void updateObject() {
    var testId = 42L;
    var testUpdateUser = Mockito.mock(UserDto.class);
    var updatedUserMock = Mockito.mock(TestLongUser.class);

    Mockito.when(userServiceMock.update(testId, testUpdateUser)).thenReturn(updatedUserMock);

    assertThat(testSubject.updateObject(testId, testUpdateUser)).isSameAs(updatedUserMock);

    Mockito.verify(userServiceMock).update(testId, testUpdateUser);
  }

  @Test
  @SuppressWarnings("unchecked")
  void update() {
    var testId = 42L;
    var updatedUserMock = Mockito.mock(TestLongUser.class);
    Map<String, Object> testUserMap = Map.of("firstName", "James");

    Mockito.when(userServiceMock.patch(testId, testUserMap)).thenReturn(updatedUserMock);

    assertThat(testSubject.update(testId, testUserMap)).isSameAs(updatedUserMock);
    Mockito.verify(userServiceMock).patch(testId, testUserMap);
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateSkipProtectedField() {
    var testId = 42L;
    var updatedUserMock = Mockito.mock(TestLongUser.class);
    Map<String, Object> testUserMap =
        Map.of(
            "firstName", "James",
            "nonce", "123456");

    ArgumentCaptor<Map<String, Object>> updateMapCaptor = ArgumentCaptor.forClass(Map.class);
    Mockito.when(userServiceMock.patch(eq(testId), updateMapCaptor.capture()))
        .thenReturn(updatedUserMock);

    assertThat(testSubject.update(testId, testUserMap)).isSameAs(updatedUserMock);
    assertThat(updateMapCaptor.getValue()).containsOnlyKeys("firstName");
    Mockito.verify(userServiceMock).patch(any(), anyMap());
  }

  @Test
  void delete() {
    var testId = 42L;

    testSubject.delete(testId);
    Mockito.verify(userServiceMock).deleteById(testId);
  }

  @Test
  void terminate() {
    var testId = 42L;
    var updatedUserMock = Mockito.mock(TestLongUser.class);

    Mockito.when(userServiceMock.patch(eq(testId), ArgumentMatchers.anyMap()))
        .thenReturn(updatedUserMock);

    testSubject.terminate(testId);

    ArgumentCaptor<Map<String, Object>> valueCaptor = ArgumentCaptor.forClass(Map.class);

    Mockito.verify(userServiceMock).patch(eq(testId), valueCaptor.capture());
    assertThat(valueCaptor.getValue()).hasSize(1);
    assertThat(valueCaptor.getValue()).containsKey("nonce");
    assertThat(valueCaptor.getValue().get("nonce")).isInstanceOf(String.class);
    assertThat((String) valueCaptor.getValue().get("nonce")).isNotEmpty();
  }

  @Test
  void getCurrentLoggedInUser() {
    var userMock = mock(TestLongUser.class);
    assertThat(testSubject.getMe(userMock)).isSameAs(userMock);
  }

  @Test
  void updateCurrentLoggedInUser() {
    UserDto updateUserMock = mock(UserDto.class);
    TestLongUser persistedUserMock = mock(TestLongUser.class);

    when(userServiceMock.selfUpdate(persistedUserMock, updateUserMock))
        .thenReturn(persistedUserMock);
    assertThat(testSubject.updateMe(persistedUserMock, updateUserMock)).isSameAs(persistedUserMock);
  }

  @Test
  void getMyTokens() {
    TestLongUser userMock = mock(TestLongUser.class);
    SessionToken mockedAccessToken = mock(SessionToken.class);
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
    when(userServiceMock.getTokens(userMock)).thenReturn(List.of(sessionToken));
    List<TokenRepresentation> myTokens = testSubject.getMyTokens(userMock);

    verify(userServiceMock, times(1)).getTokens(userMock);
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
    TestLongUser userMock = mock(TestLongUser.class);
    UUID tokenId = UUID.randomUUID();
    testSubject.deleteToken(userMock, tokenId);
    verify(userServiceMock, times(1)).deleteToken(userMock, tokenId);
    verifyNoMoreInteractions(userServiceMock);
  }
}
