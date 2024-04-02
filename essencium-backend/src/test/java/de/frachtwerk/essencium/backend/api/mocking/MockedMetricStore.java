package de.frachtwerk.essencium.backend.api.mocking;

import java.util.*;

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
