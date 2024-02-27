/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.service.ContactMailService;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactUUIDControllerTest {

  private final ContactMailService<TestUUIDUser, UUID> contactServiceMock =
      mock(ContactMailService.class);

  private final ContactController testSubject = new ContactController(contactServiceMock);

  @BeforeEach
  void setUp() {
    reset(contactServiceMock);
  }

  @Nested
  class SendContactRequest {

    private final ContactRequestDto testRequest = mock(ContactRequestDto.class);

    @Test
    void sendContactRequest_currentUserNull() throws CheckedMailException {

      doAnswer(
              invocationOnMock -> {
                final ContactRequestDto contactRequest = invocationOnMock.getArgument(0);
                final TestUUIDUser issuingUser = invocationOnMock.getArgument(1);

                assertThat(contactRequest).isSameAs(testRequest);
                assertThat(issuingUser).isNull();

                return "";
              })
          .when(contactServiceMock)
          .sendContactRequest(any(), any());

      testSubject.sendContactRequest(testRequest, null);
    }

    @SneakyThrows
    @Test
    void sendContactRequest_currentUserNotNull() {
      var testUser = mock(TestUUIDUser.class);

      doAnswer(
              invocationOnMock -> {
                final ContactRequestDto contactRequest = invocationOnMock.getArgument(0);
                final TestUUIDUser issuingUser = invocationOnMock.getArgument(1);

                assertThat(contactRequest).isSameAs(testRequest);
                assertThat(issuingUser).isSameAs(testUser);

                return "";
              })
          .when(contactServiceMock)
          .sendContactRequest(any(), any());

      testSubject.sendContactRequest(testRequest, testUser);
    }
  }
}
