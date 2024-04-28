package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.service.ContactMailService;
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

class ContactControllerTest {
  @Mock ContactMailService contactService;
  @InjectMocks ContactController contactController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSendContactRequest() throws CheckedMailException {
    contactController.sendContactRequest(
        new ContactRequestDto("name", "mailAddress", "subject", "message"), null);
    verify(contactService)
        .sendContactRequest(any(ContactRequestDto.class), any(AbstractBaseUser.class));
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = contactController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = contactController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
