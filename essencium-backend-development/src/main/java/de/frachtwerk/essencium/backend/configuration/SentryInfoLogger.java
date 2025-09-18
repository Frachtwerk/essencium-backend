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
