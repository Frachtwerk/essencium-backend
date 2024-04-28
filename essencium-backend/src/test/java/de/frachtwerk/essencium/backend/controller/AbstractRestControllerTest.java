package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AbstractRestControllerTest {
  @Mock AbstractEntityService<?, ?, ?> service;
  @InjectMocks AbstractRestController abstractRestController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /*
  @Test
  void testFindById(){
      when(service.getById(any(ID.class))).thenReturn(new OUT());

      AbstractBaseModel result = abstractRestController.findById(new Translation.TranslationId());
      Assertions.assertEquals(new UUIDModel("createdBy", "updatedBy", LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0), LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0)), result);
  }

  @Test
  void testCreate(){
      when(service.create(any(E.class))).thenReturn(new OUT());

      AbstractBaseModel result = abstractRestController.create("input");
      Assertions.assertEquals(new UUIDModel("createdBy", "updatedBy", LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0), LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0)), result);
  }

  @Test
  void testUpdateObject(){
      when(service.update(any(ID.class), any(E.class))).thenReturn(new OUT());

      AbstractBaseModel result = abstractRestController.updateObject(new Translation.TranslationId(), "input");
      Assertions.assertEquals(new UUIDModel("createdBy", "updatedBy", LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0), LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0)), result);
  }

  @Test
  void testPatch(){
      when(service.patch(any(), any(Map.class))).thenReturn(any());

      AbstractBaseModel result = abstractRestController.patch(new Translation.TranslationId(), Map.of("fields","fields"));
      Assertions.assertEquals(new UUIDModel("createdBy", "updatedBy", LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0), LocalDateTime.of(2024, Month.APRIL, 24, 10, 29, 0)), result);
  }

   */

  @Test
  void testDelete() {
    abstractRestController.delete(new Translation.TranslationId());
    verify(service).deleteById(any());
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = abstractRestController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    Assertions.assertTrue(
        result
            .getHeaders()
            .getAllow()
            .containsAll(
                Set.of(
                    HttpMethod.GET,
                    HttpMethod.HEAD,
                    HttpMethod.POST,
                    HttpMethod.PUT,
                    HttpMethod.PATCH,
                    HttpMethod.DELETE,
                    HttpMethod.OPTIONS)));
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = abstractRestController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
