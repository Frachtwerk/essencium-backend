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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.exception.UnauthorizedException;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import io.jsonwebtoken.ProtectedHeader;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class SessionTokenKeyLocatorTest {

  @Mock private SessionTokenRepository sessionTokenRepositoryMock;
  @InjectMocks private SessionTokenKeyLocator testSubject;

  @Test
  void locateFail() {
    ProtectedHeader protectedHeader = mock(ProtectedHeader.class);
    UUID uuid = UUID.randomUUID();

    when(protectedHeader.getKeyId()).thenReturn(uuid.toString());
    when(sessionTokenRepositoryMock.getSessionTokenById(uuid)).thenReturn(null);

    assertThrows(UnauthorizedException.class, () -> testSubject.locate(protectedHeader));

    verify(sessionTokenRepositoryMock, times(1)).getSessionTokenById(uuid);
    verifyNoMoreInteractions(sessionTokenRepositoryMock);
  }

  @Test
  void locate() {
    ProtectedHeader protectedHeader = mock(ProtectedHeader.class);
    UUID uuid = UUID.randomUUID();
    SessionToken sessionToken =
        SessionToken.builder()
            .id(uuid)
            .key(mock(SecretKey.class))
            .username("test@example.com")
            .type(SessionTokenType.REFRESH)
            .issuedAt(new Date())
            .expiration(new Date())
            .userAgent("test")
            .accessTokens(List.of(SessionToken.builder().id(UUID.randomUUID()).build()))
            .build();

    when(protectedHeader.getKeyId()).thenReturn(uuid.toString());
    when(sessionTokenRepositoryMock.getSessionTokenById(uuid)).thenReturn(sessionToken);

    SecretKey secretKey = testSubject.locate(protectedHeader);

    assertNotNull(secretKey);
    assertEquals(sessionToken.getId(), uuid);
    verify(sessionTokenRepositoryMock, times(1)).getSessionTokenById(uuid);
    verifyNoMoreInteractions(sessionTokenRepositoryMock);
  }
}
