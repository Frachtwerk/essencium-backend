package de.frachtwerk.essencium.backend.service;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Feedback;
import io.sentry.Sentry;
import io.sentry.UserFeedback;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

  @InjectMocks FeedbackService feedbackService;

  @Test
  void sendFeedback_shouldCaptureUserFeedback() {
    Feedback feedback = mock(Feedback.class);
    when(feedback.getEventId()).thenReturn(UUID.randomUUID().toString());
    when(feedback.getName()).thenReturn("Max Mustermann");
    when(feedback.getEmail()).thenReturn("max@example.com");
    when(feedback.getComments()).thenReturn("Super!");

    try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
      feedbackService.sendFeedback(feedback);

      sentryMock.verify(() -> Sentry.captureUserFeedback(any(UserFeedback.class)), times(1));
    }
  }
}
