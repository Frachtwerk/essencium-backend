package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Feedback;
import de.frachtwerk.essencium.backend.service.FeedbackService;
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

class SentryProxyControllerTest {
  @Mock FeedbackService feedbackService;
  @InjectMocks SentryProxyController sentryProxyController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSendFeedback() {
    Feedback result = sentryProxyController.sendFeedback(null);
    verify(feedbackService).sendFeedback(any(Feedback.class));
    Assertions.assertEquals(null, result);
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = sentryProxyController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = sentryProxyController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
