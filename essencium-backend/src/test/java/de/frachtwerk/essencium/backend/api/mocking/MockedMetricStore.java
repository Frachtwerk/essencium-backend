package de.frachtwerk.essencium.backend.api.mocking;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

  private static final Map<String, Set<String>> SENT_MAILS_WITH_PARAMS = new HashMap<>();

  private MockedMetricStore() {}

  public static MockedMetricStore getInstance() {
    return INSTANCE;
  }

  public void clearStore() {
    SENT_MAILS_WITH_PARAMS.clear();
  }

  public void storeSentMailWithParam(String recipient, Set<String> parameters) {
    SENT_MAILS_WITH_PARAMS.put(recipient, parameters);
  }

  public Set<String> getMailParametersForRecipient(String recipient) {
    if (!SENT_MAILS_WITH_PARAMS.containsKey(recipient)) {
      return Collections.emptySet();
    }

    return new HashSet<>(SENT_MAILS_WITH_PARAMS.get(recipient));
  }

  public int getTotalSentMails() {
    return SENT_MAILS_WITH_PARAMS.size();
  }
}
