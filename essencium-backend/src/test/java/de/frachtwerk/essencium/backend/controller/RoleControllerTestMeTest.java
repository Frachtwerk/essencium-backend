package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.RoleDto;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RoleControllerTestMeTest {
  @Mock RoleService roleService;
  @InjectMocks RoleController roleController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testFindAll() {
    when(roleService.getAll(any(Pageable.class))).thenReturn(null);

    Page<Role> result = roleController.findAll(null);
    Assertions.assertEquals(null, result);
  }

  @Test
  void testFindById() {
    when(roleService.getByName(anyString()))
        .thenReturn(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description"))));

    Role result = roleController.findById("id");
    Assertions.assertEquals(
        new Role(
            "name", "description", true, true, true, Set.of(new Right("authority", "description"))),
        result);
  }

  @Test
  void testCreate() {
    when(roleService.getByName(anyString()))
        .thenReturn(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description"))));
    when(roleService.save(any(RoleDto.class)))
        .thenReturn(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description"))));

    Role result =
        roleController.create(new RoleDto("name", "description", true, true, Set.of("rights")));
    Assertions.assertEquals(
        new Role(
            "name", "description", true, true, true, Set.of(new Right("authority", "description"))),
        result);
  }

  @Test
  void testUpdateObject() {
    when(roleService.save(any(RoleDto.class)))
        .thenReturn(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description"))));

    Role result =
        roleController.updateObject(
            "name", new RoleDto("name", "description", true, true, Set.of("rights")));
    Assertions.assertEquals(
        new Role(
            "name", "description", true, true, true, Set.of(new Right("authority", "description"))),
        result);
  }

  @Test
  void testUpdate() {
    when(roleService.patch(anyString(), any(Map.class)))
        .thenReturn(
            new Role(
                "name",
                "description",
                true,
                true,
                true,
                Set.of(new Right("authority", "description"))));

    Role result = roleController.update("name", Map.of("roleFields", "roleFields"));
    Assertions.assertEquals(
        new Role(
            "name", "description", true, true, true, Set.of(new Right("authority", "description"))),
        result);
  }

  @Test
  void testDelete() {
    roleController.delete("name");
    verify(roleService).deleteById(anyString());
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = roleController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = roleController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
