package de.frachtwerk.essencium.backend.test.integration.util.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// https://github.com/tomakehurst/wiremock/issues/684#issuecomment-621566363
public class WireMockExtension extends WireMockServer
    implements BeforeEachCallback, AfterEachCallback {

  public WireMockExtension(int port) {
    super(port);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    this.start();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    this.stop();
    this.resetAll();
  }
}
