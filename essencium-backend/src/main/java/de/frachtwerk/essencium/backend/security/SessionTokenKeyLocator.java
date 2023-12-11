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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.exception.UnauthorizedException;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import java.security.Key;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionTokenKeyLocator extends LocatorAdapter<Key> {
  private final SessionTokenRepository sessionTokenRepository;

  @Autowired
  public SessionTokenKeyLocator(SessionTokenRepository sessionTokenRepository) {
    this.sessionTokenRepository = sessionTokenRepository;
  }

  @Override
  protected SecretKey locate(ProtectedHeader header) {
    String keyId = header.getKeyId();
    if (keyId == null) {
      throw new UnauthorizedException("Session token not found. Session expired?");
    }
    UUID uuid = UUID.fromString(keyId);
    SessionToken sessionToken = sessionTokenRepository.getSessionTokenById(uuid);
    if (sessionToken == null) {
      throw new UnauthorizedException("Session token not found. Session expired?");
    }
    return sessionToken.getKey();
  }
}
