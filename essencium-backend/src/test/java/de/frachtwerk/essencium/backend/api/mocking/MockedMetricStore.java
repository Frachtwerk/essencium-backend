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
