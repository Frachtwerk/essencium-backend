package de.frachtwerk.essencium.backend.api.data.extension;

import de.frachtwerk.essencium.backend.api.mocking.MockedMetricStore;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MetricCleanUpExtension implements AfterEachCallback {
  @Override
  public void afterEach(ExtensionContext extensionContext) {
    MockedMetricStore.getInstance().clearStore();
  }
}
