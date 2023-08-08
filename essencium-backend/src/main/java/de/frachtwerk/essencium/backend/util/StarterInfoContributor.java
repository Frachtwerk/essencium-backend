/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class StarterInfoContributor implements InfoContributor {

  private static final String DEFAULT_VERSION = "unknown";

  @Value("${pom.version:#{null}}")
  private Optional<String> version;

  private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

  @Override
  public void contribute(Info.Builder builder) {
    builder
        .withDetail("version", version.orElse(DEFAULT_VERSION))
        .withDetail(
            "upstreamVersion",
            Optional.ofNullable(
                    StarterInfoContributor.class.getPackage().getImplementationVersion())
                .orElse(DEFAULT_VERSION))
        .withDetail("javaVersion", System.getProperty("java.version"))
        .withDetail("serverTime", LocalDateTime.now())
        .withDetail("uptime", Duration.ofMillis(runtimeMXBean.getUptime()));
  }
}
