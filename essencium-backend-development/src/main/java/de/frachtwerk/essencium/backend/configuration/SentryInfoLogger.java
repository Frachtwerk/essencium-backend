package de.frachtwerk.essencium.backend.configuration;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

@Configuration
@Profile("sentry-debug")
public class SentryInfoLogger {
  @EventListener(ApplicationReadyEvent.class)
  public void printJVMProperties() {
    Properties properties = System.getProperties();
    SentryEvent sentryEvent =
        new SentryEvent(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
    Message message = new Message();
    message.setMessage("SpringBootApp started");
    sentryEvent.setMessage(message);
    properties
        .stringPropertyNames()
        .forEach(key -> sentryEvent.setExtra(key, properties.getProperty(key)));
    Sentry.captureEvent(sentryEvent);
  }
}
