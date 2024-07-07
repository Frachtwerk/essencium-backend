package de.frachtwerk.essencium.backend.api.assertions;

import static org.mockito.Mockito.verifyNoInteractions;

import de.frachtwerk.essencium.backend.api.mocking.MockedMetricStore;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.AbstractAssert;

public class MailAssert extends AbstractAssert<MailAssert, UserMailService> {
  public MailAssert(UserMailService actual) {
    super(actual, MailAssert.class);
  }

  public void hasSentNoMails() {
    verifyNoInteractions(actual);
  }

  public MailAssert hasSentInTotal(int amountOfMails) {
    int totalSentMails = MockedMetricStore.getInstance().getTotalSentMails();

    if (totalSentMails != amountOfMails) {
      failWithActualExpectedAndMessage(
          totalSentMails, amountOfMails, "More or less mails where actually sent than expected");
    }

    return this;
  }

  public MailAssertAdditions hasSentAMailTo(String recipient) {
    Map<LocalDateTime, Set<String>> sendMailParametersForRecipient =
        MockedMetricStore.getInstance().getMailParametersForRecipient(recipient);
    if (sendMailParametersForRecipient == null) {
      failWithMessage("No message was sent to %s", recipient);
    }

    return new MailAssertAdditions(recipient, sendMailParametersForRecipient);
  }

  public class MailAssertAdditions {

    private final String recipient;
    private final Map<LocalDateTime, Set<String>> mailParameters;

    private MailAssertAdditions(String recipient, Map<LocalDateTime, Set<String>> mailParameters) {
      this.recipient = recipient;
      this.mailParameters = mailParameters;
    }

    public MailAssertAdditions withParameterInLastSendMail(String expectedParameter) {

      LocalDateTime lastDate =
          this.mailParameters.keySet().stream().max(LocalDateTime::compareTo).get();

      if (!mailParameters.get(lastDate).contains(expectedParameter)) {
        failWithMessage("The mail parameters do not contain %s", expectedParameter);
      }

      return this;
    }
  }
}
