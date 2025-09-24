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
