package de.frachtwerk.essencium.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AbstractDefaultUnpagedRestControllerTest {
  @Mock AbstractEntityService<?, ?, ?> service;
  @InjectMocks AbstractDefaultUnpagedRestController abstractDefaultUnpagedRestController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testFindAll() {
    when(service.getAll()).thenReturn(List.of(any()));

    List result = abstractDefaultUnpagedRestController.findAll();
    assertEquals(List.of("replaceMeWithExpectedResult"), result);
  }

  @Test
  void testFindById() {
    when(service.getById(any())).thenReturn(any());

    AbstractBaseModel result = abstractDefaultUnpagedRestController.findById(any());
    assertEquals(any(), result);
  }

  @Test
  void testCreate() {
    when(service.create(any())).thenReturn(any());

    AbstractBaseModel result = abstractDefaultUnpagedRestController.create(any());
    assertEquals(any(), result);
  }

  @Test
  void testUpdateObject() {
    when(service.update(any(), any())).thenReturn(any());

    AbstractBaseModel result = abstractDefaultUnpagedRestController.updateObject(any(), any());
    assertEquals(any(), result);
  }

  @Test
  void testPatch() {
    when(service.patch(any(), any(Map.class))).thenReturn(any());

    AbstractBaseModel result =
        abstractDefaultUnpagedRestController.patch(any(), Map.of("fields", "fields"));
    assertEquals(any(), result);
  }

  @Test
  void testDelete() {
    abstractDefaultUnpagedRestController.delete(any());
    verify(service).deleteById(any());
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = abstractDefaultUnpagedRestController.collectionOptions();
    assertEquals(new ResponseEntity<>(HttpStatus.OK), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
