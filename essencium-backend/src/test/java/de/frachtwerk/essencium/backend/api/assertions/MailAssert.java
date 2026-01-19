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

package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.verifyNoInteractions;

import de.frachtwerk.essencium.backend.api.mocking.MockedMetricStore;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.util.Set;
import org.assertj.core.api.AbstractAssert;

public class MailAssert extends AbstractAssert<MailAssert, UserMailService> {
  public MailAssert(UserMailService actual) {
    super(actual, MailAssert.class);
  }

  public void hasSentNoMails() {
    verifyNoInteractions(actual);
  }

  public void sentInTotalMails(int amount) {
    int totalSentMails = MockedMetricStore.getInstance().getTotalSentMails();

    if (totalSentMails != amount) {
      failWithActualExpectedAndMessage(
          totalSentMails, actual, "More or less mails where actually sent than expected");
    }
  }

  public MailAssertAdditions hasSentAMailTo(String recipient) {
    Set<String> mailParametersForRecipient =
        MockedMetricStore.getInstance().getMailParametersForRecipient(recipient);
    if (mailParametersForRecipient == null) {
      failWithMessage("No message was sent to %s", recipient);
    }

    return new MailAssertAdditions(recipient, mailParametersForRecipient);
  }

  public class MailAssertAdditions {

    private final String recipient;
    private final Set<String> mailParameters;

    private MailAssertAdditions(String recipient, Set<String> mailParameters) {
      this.recipient = recipient;
      this.mailParameters = mailParameters;
    }

    public MailAssertAdditions withParameter(String expectedParameter) {
      if (!mailParameters.contains(expectedParameter)) {
        failWithMessage("The mail parameters do not contain %s", expectedParameter);
      }

      return this;
    }
  }
}
