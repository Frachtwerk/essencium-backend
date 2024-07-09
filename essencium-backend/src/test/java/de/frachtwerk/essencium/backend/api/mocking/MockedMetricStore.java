package de.frachtwerk.essencium.backend.api.mocking;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The MockedMetricStore class represents a singleton store which should be used during jUnit test
 * executions. The store should be used to track interactions with mocked services - especially to
 * store passed parameters to mocked methods, e.g. {@link
 * MailServiceMockConfiguration#trackNewUserMailSend()} <br>
 * Unit tests classes, which use the {@link MockConfig)}, should be annotated with {@link
 * de.frachtwerk.essencium.backend.api.annotations.EssenciumUnitTest}. This ensures, that the {@link
 * de.frachtwerk.essencium.backend.api.data.extension.MetricCleanUpExtension} cleans the store after
 * every test, so that the tests do not erroneously influence each other.
 */
public class MockedMetricStore {

  private static final MockedMetricStore INSTANCE = new MockedMetricStore();

  private static final Map<String, Map<LocalDateTime, Set<String>>> SENT_MAILS_WITH_PARAMS =
      new HashMap<>();

  private MockedMetricStore() {}

  public static MockedMetricStore getInstance() {
    return INSTANCE;
  }

  public void clearStore() {
    SENT_MAILS_WITH_PARAMS.clear();
  }

  public void storeSentMailWithParam(String recipient, Set<String> parameters) {
    if (!SENT_MAILS_WITH_PARAMS.containsKey(recipient)) {
      SENT_MAILS_WITH_PARAMS.put(recipient, new HashMap<>());
    }

    SENT_MAILS_WITH_PARAMS.get(recipient).put(LocalDateTime.now(), parameters);
  }

  public HashMap<LocalDateTime, Set<String>> getMailParametersForRecipient(String recipient) {
    if (!SENT_MAILS_WITH_PARAMS.containsKey(recipient)) {
      return null;
    }

    Map<LocalDateTime, Set<String>> allSendMailsToRecipient = SENT_MAILS_WITH_PARAMS.get(recipient);

    return new HashMap<>(allSendMailsToRecipient);
  }

  public int getTotalSentMails() {
    return SENT_MAILS_WITH_PARAMS.values().stream().mapToInt(Map::size).sum();
  }
}
