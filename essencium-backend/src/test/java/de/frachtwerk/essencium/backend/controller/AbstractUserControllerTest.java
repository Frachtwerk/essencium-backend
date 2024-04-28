package de.frachtwerk.essencium.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.AbstractRepresentationAssembler;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;

class AbstractUserControllerTest {
  @Mock Set<String> PROTECTED_USER_FIELDS;
  @Mock AbstractRepresentationAssembler<?, ?> assembler;
  @Mock AbstractUserService<?, ?, ?> userService;
  @InjectMocks AbstractUserController abstractUserController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testFindAll() {
    when(userService.getAllFiltered(any(Specification.class), any(Pageable.class)))
        .thenReturn(null);

    Page result = abstractUserController.findAll(null, null);
    assertEquals(null, result);
  }

  @Test
  void testFindById() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.getById(any())).thenReturn(any());

    Object result = abstractUserController.findById(new Translation.TranslationId());
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testCreate() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.loadUserByUsername(anyString())).thenReturn(any());
    when(userService.create(any())).thenReturn(any());

    Object result = abstractUserController.create(new UserDto());
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testUpdateObject() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.update(any(), any())).thenReturn(any());

    Object result =
        abstractUserController.updateObject(new Translation.TranslationId(), new UserDto());
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testUpdate() {
    when(PROTECTED_USER_FIELDS.contains(any(Object.class))).thenReturn(true);
    when(PROTECTED_USER_FIELDS.stream()).thenReturn(null);
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.patch(any(), any(Map.class))).thenReturn(any());

    Object result =
        abstractUserController.update(
            new Translation.TranslationId(), Map.of("userFields", "userFields"));
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testDelete() {
    abstractUserController.delete(new Translation.TranslationId());
    verify(userService).deleteById(any());
  }

  @Test
  void testTerminate() {
    when(userService.generateNonce()).thenReturn("generateNonceResponse");
    when(userService.patch(any(), any(Map.class))).thenReturn(any());

    abstractUserController.terminate(new Translation.TranslationId());
  }

  @Test
  void testGetMe() {
    when(assembler.toModel(any())).thenReturn(any());

    Object result = abstractUserController.getMe(null);
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testUpdateMe() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.selfUpdate(any(), (Map<String, Object>) any())).thenReturn(any());

    Object result = abstractUserController.updateMe(null, new UserDto());
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testUpdateMePartial() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.selfUpdate(any(), any(Map.class))).thenReturn(any());

    Object result =
        abstractUserController.updateMePartial(null, Map.of("userFields", "userFields"));
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testUpdatePassword() {
    when(assembler.toModel(any())).thenReturn(any());
    when(userService.updatePassword(any(), any(PasswordUpdateRequest.class))).thenReturn(any());

    Object result =
        abstractUserController.updatePassword(
            null, new PasswordUpdateRequest("password", "verification"));
    assertEquals("replaceMeWithExpectedResult", result);
  }

  @Test
  void testGetMyRoleOld() {
    Set<Role> result = abstractUserController.getMyRole(null);
    assertEquals(
        Set.of(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description")))),
        result);
  }

  @Test
  void testGetMyRightsOld() {
    Collection<GrantedAuthority> result = abstractUserController.getMyRights(null);
    assertEquals(
        List.of(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description")))),
        result);
  }

  @Test
  void testGetMyRole() {
    Set<Role> result = abstractUserController.getMyRole(null);
    assertEquals(
        Set.of(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description")))),
        result);
  }

  @Test
  void testGetMyRights() {
    Collection<GrantedAuthority> result = abstractUserController.getMyRights(null);
    assertEquals(
        List.of(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description")))),
        result);
  }

  @Test
  void testGetMyTokens() {
    when(PROTECTED_USER_FIELDS.stream()).thenReturn(null);
    when(userService.getTokens(any())).thenReturn(List.of(new SessionToken()));

    List<TokenRepresentation> result = abstractUserController.getMyTokens(null);
    assertEquals(
        List.of(
            new TokenRepresentation(
                new UUID(0L, 0L),
                SessionTokenType.ACCESS,
                new GregorianCalendar(2024, Calendar.APRIL, 24, 10, 20).getTime(),
                new GregorianCalendar(2024, Calendar.APRIL, 24, 10, 20).getTime(),
                "userAgent",
                LocalDateTime.of(2024, Month.APRIL, 24, 10, 20, 20))),
        result);
  }

  @Test
  void testDeleteToken() {
    abstractUserController.deleteToken(null, new UUID(0L, 0L));
    verify(userService).deleteToken(any(), any(UUID.class));
  }

  @Test
  void testCollectionOptions() {
    when(PROTECTED_USER_FIELDS.toArray(any(Object[].class))).thenReturn(new Object[] {});
    when(Set.of(any(), any(), any(), any(), any(), any(), any())).thenReturn(Set.of());

    ResponseEntity<Object> result = abstractUserController.collectionOptions();
    assertEquals(new ResponseEntity<>(HttpStatus.OK), result);
  }

  @Test
  void testGetAllowedMethods() {
    when(Set.of(any(), any(), any(), any(), any(), any(), any())).thenReturn(Set.of(null));

    Set<HttpMethod> result = abstractUserController.getAllowedMethods();
    assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
