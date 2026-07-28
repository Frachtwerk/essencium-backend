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

package de.frachtwerk.essencium.backend.configuration.properties.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Local (username/password) security policy bound from the {@code app.security.*} namespace.
 *
 * <p>These properties govern password quality enforcement and brute-force protection for the
 * built-in credential login. They are independent of the external auth mechanisms (OAuth2/LDAP) and
 * only apply to locally managed accounts.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

  /**
   * Minimum acceptable password strength on the zxcvbn scale {@code 0}–{@code 4}, enforced by the
   * {@code StrongPasswordValidator}. Default: {@code 4} (strongest). A password whose measured
   * score is below this value is rejected.
   */
  private int minPasswordStrength = 4;

  /**
   * Number of consecutive failed login attempts after which an account is locked by the {@code
   * BruteForceProtectionService}. Default: {@code 10}.
   */
  private int maxFailedLogins = 10;
}
