package de.frachtwerk.essencium.backend.api.mocking;

import java.util.*;

public class MockedMetricStore {

  private static final MockedMetricStore INSTANCE = new MockedMetricStore();

  private static final Map<String, Set<String>> SEND_MAILS_WITH_PARAMS = new HashMap<>();

  private MockedMetricStore() {}

  public static MockedMetricStore getInstance() {
    return INSTANCE;
  }

  public void clearSendMails() {
    SEND_MAILS_WITH_PARAMS.clear();
  }

  public void storeSendMailWithParam(String recipient, Set<String> parameters) {
    SEND_MAILS_WITH_PARAMS.put(recipient, parameters);
  }

  public Set<String> getMailParametersForRecipient(String recipient) {
    if (!SEND_MAILS_WITH_PARAMS.containsKey(recipient)) {
      return Collections.emptySet();
    }

    return new HashSet<>(SEND_MAILS_WITH_PARAMS.get(recipient));
  }

  public int getTotalSendMails() {
    return SEND_MAILS_WITH_PARAMS.size();
  }
}
