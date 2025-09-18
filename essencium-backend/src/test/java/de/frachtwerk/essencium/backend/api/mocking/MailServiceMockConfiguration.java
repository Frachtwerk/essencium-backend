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

package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import de.frachtwerk.essencium.backend.service.UserMailService;
import java.util.Set;
import org.springframework.mail.MailException;

public class MailServiceMockConfiguration implements MockConfiguration {

  private final UserMailService mockedObject;

  public MailServiceMockConfiguration(UserMailService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public MailServiceMockConfiguration trackNewUserMailSend() {
    try {
      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                MockedMetricStore.getInstance().storeSentMailWithParam(mail, Set.of(token));

                return "";
              })
          .when(mockedObject)
          .sendNewUserMail(anyString(), anyString(), any());
    } catch (MailException e) {
      throw new RuntimeException(e);
    }

    return this;
  }

  public MailServiceMockConfiguration trackResetTokenSend() {
    try {
      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                MockedMetricStore.getInstance().storeSentMailWithParam(mail, Set.of(token));

                return "";
              })
          .when(mockedObject)
          .sendResetToken(anyString(), anyString(), any());
    } catch (MailException e) {
      throw new RuntimeException(e);
    }

    return this;
  }
}
