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

package de.frachtwerk.essencium.backend.security.event;

import de.frachtwerk.essencium.backend.security.BruteForceProtectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener
    implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFailureListener.class);

  private final BruteForceProtectionService bruteForceProtectionService;

  @Override
  public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
    String username = event.getAuthentication().getName();
    LOGGER.info("********* Login failed for user {} ", username);
    bruteForceProtectionService.registerLoginFailure(username);
  }
}
