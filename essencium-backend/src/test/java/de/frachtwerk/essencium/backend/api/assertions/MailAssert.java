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
