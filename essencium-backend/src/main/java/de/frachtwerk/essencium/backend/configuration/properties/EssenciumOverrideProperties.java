/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Feature flags for disabling Essencium's built-in REST controllers, bound from the {@code
 * essencium.overrides.*} namespace.
 *
 * <p>Each flag corresponds to a controller that is registered via
 * {@code @ConditionalOnProperty(havingValue = "false", matchIfMissing = true)}. Consequently the
 * default ({@code false}) keeps the built-in controller <b>active</b>; setting a flag to {@code
 * true} removes the default controller so that a downstream application can provide its own
 * implementation of the same endpoints without a bean/mapping conflict.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Configuration
@ConfigurationProperties(prefix = "essencium.overrides")
public class EssenciumOverrideProperties {
  /**
   * When {@code true}, disables the built-in {@code AuthenticationController} ({@code /auth}).
   * Default: {@code false} (controller active).
   */
  private boolean authController = false;

  /**
   * When {@code true}, disables the built-in {@code ContactController} ({@code /contact}). Default:
   * {@code false} (controller active). See also {@link MailProperties.ContactMail}.
   */
  private boolean contactController = false;

  /**
   * When {@code true}, disables the built-in {@code ResetCredentialsController}. Default: {@code
   * false} (controller active).
   */
  private boolean resetCredentialsController = false;

  /**
   * When {@code true}, disables the built-in {@code RightController} ({@code /rights}). Default:
   * {@code false} (controller active).
   */
  private boolean rightController = false;

  /**
   * When {@code true}, disables the built-in {@code SentryProxyController}. Default: {@code false}
   * (controller active). Typically only meaningful together with a valid {@link SentryProperties}
   * configuration.
   */
  private boolean sentryProxyController = false;

  /**
   * When {@code true}, disables the built-in {@code TranslationController}. Default: {@code false}
   * (controller active).
   */
  private boolean translationController = false;
}
