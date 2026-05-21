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

package de.frachtwerk.essencium.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppTokenProperties;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtTokenAuthenticationFilterTest {

  @Mock private JwtTokenService jwtTokenService;
  @Mock private AppTokenProperties appTokenProperties;

  @Mock private Jws<Claims> jws;
  @Mock private JwsHeader jwsHeader;
  @Mock private Claims claims;

  private JwtTokenAuthenticationFilter<Long> filter;

  @BeforeEach
  void setUp() {
    filter = new JwtTokenAuthenticationFilter<>(request -> true);
    ReflectionTestUtils.setField(filter, "jwtTokenService", jwtTokenService);
    ReflectionTestUtils.setField(filter, "appTokenProperties", appTokenProperties);
  }

  @Nested
  class ExtractBearerToken {

    @Test
    void validToken_returnsTokenString() {
      assertThat(JwtTokenAuthenticationFilter.extractBearerToken("Bearer aaa.bbb.ccc"))
          .isEqualTo("aaa.bbb.ccc");
    }

    @Test
    void missingBearerPrefix_throws() {
      assertThatThrownBy(() -> JwtTokenAuthenticationFilter.extractBearerToken("aaa.bbb.ccc"))
          .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void emptyInput_throws() {
      assertThatThrownBy(() -> JwtTokenAuthenticationFilter.extractBearerToken(""))
          .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void twoPartToken_throws() {
      assertThatThrownBy(() -> JwtTokenAuthenticationFilter.extractBearerToken("Bearer aaa.bbb"))
          .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
  }

  @Nested
  class ResolveClientIp {

    static Stream<Arguments> cases() {
      return Stream.of(
          Arguments.of("no proxies — XFF ignored", Set.of(), "1.2.3.4", "9.9.9.9", "1.2.3.4"),
          Arguments.of(
              "no proxies — multi-hop XFF ignored",
              Set.of(),
              "1.2.3.4",
              "5.5.5.5, 6.6.6.6",
              "1.2.3.4"),
          Arguments.of("no proxies — null XFF", Set.of(), "1.2.3.4", null, "1.2.3.4"),
          Arguments.of(
              "trusted proxy — first non-trusted",
              Set.of("10.0.0.1"),
              "10.0.0.1",
              "1.2.3.4",
              "1.2.3.4"),
          Arguments.of(
              "all hops trusted — fallback leftmost",
              Set.of("10.0.0.1", "10.0.0.2"),
              "10.0.0.1",
              "10.0.0.2",
              "10.0.0.2"),
          Arguments.of(
              "trusted CIDR — client outside CIDR",
              Set.of("10.0.0.0/8"),
              "10.0.0.1",
              "1.2.3.4",
              "1.2.3.4"),
          Arguments.of(
              "trusted proxy — no XFF, direct client",
              Set.of("10.0.0.1"),
              "5.5.5.5",
              null,
              "5.5.5.5"),
          Arguments.of(
              "IPv4-only trusted-proxies vs IPv6 loopback: XFF is NOT honoured",
              Set.of("127.0.0.1"), // trustedProxies (IPv4 only)
              "::1", // remoteAddr (dual-stack default)
              "192.0.2.99", // XFF claims the real client
              "::1"), // resolved IP = ::1, NOT 192.0.2.99
          Arguments.of(
              "Both v4 + v6 listed: XFF is honoured",
              Set.of("127.0.0.1", "::1"),
              "::1",
              "192.0.2.99",
              "192.0.2.99") // real client correctly resolved
          );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void resolveClientIp(
        String description,
        Set<String> trustedProxies,
        String remoteAddr,
        String xForwardedFor,
        String expectedIp) {
      when(appTokenProperties.getTrustedProxies()).thenReturn(trustedProxies);
      assertThat(filter.resolveClientIp(remoteAddr, xForwardedFor)).isEqualTo(expectedIp);
    }
  }

  @Nested
  @MockitoSettings(strictness = Strictness.LENIENT)
  class IpWhitelisting {

    @BeforeEach
    void setUpApiTokenJws() {
      when(jwtTokenService.verifyToken(anyString())).thenReturn(jws);
      when(jws.getHeader()).thenReturn(jwsHeader);
      when(jws.getPayload()).thenReturn(claims);
      when(jwsHeader.getType()).thenReturn(SessionTokenType.API.name());
      // Claims needs entrySet() for Map.copyOf() and untyped get() for createPrincipal()
      when(claims.entrySet()).thenReturn(Set.of());
      when(claims.get(JwtTokenService.CLAIM_ROLES)).thenReturn(List.of());
      when(claims.get(JwtTokenService.CLAIM_RIGHTS)).thenReturn(List.of());
    }

    @Test
    void noWhitelistConfigured_anyIpIsAllowed() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("9.9.9.9");

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void allowedIp_passes() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("1.2.3.4"));
      when(appTokenProperties.getTrustedProxies()).thenReturn(Set.of());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("1.2.3.4");

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void deniedIp_throws() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("1.2.3.4"));
      when(appTokenProperties.getTrustedProxies()).thenReturn(Set.of());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("9.9.9.9");

      assertThatThrownBy(() -> filter.getAuthentication("token", request))
          .isInstanceOf(ApiTokenConstraintViolationAuthenticationException.class)
          .hasMessageContaining("IP address not allowed");
    }

    @Test
    void allowedCidr_passes() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("192.168.1.0/24"));
      when(appTokenProperties.getTrustedProxies()).thenReturn(Set.of());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("192.168.1.100");

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void blankRemoteAddr_throws() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("1.2.3.4"));
      when(appTokenProperties.getTrustedProxies()).thenReturn(Set.of());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("");

      assertThatThrownBy(() -> filter.getAuthentication("token", request))
          .isInstanceOf(ApiTokenConstraintViolationAuthenticationException.class)
          .hasMessageContaining("Unable to determine remote IP");
    }

    @Test
    void nonApiToken_skipsIpCheck() {
      when(jwsHeader.getType()).thenReturn(SessionTokenType.ACCESS.name());
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("1.2.3.4"));
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("9.9.9.9"); // would be rejected as API token

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void clientBehindTrustedProxy_usesXForwardedFor() {
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of("1.2.3.4"));
      when(appTokenProperties.getTrustedProxies()).thenReturn(Set.of("10.0.0.1"));
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("10.0.0.1"); // trusted proxy
      request.addHeader("X-Forwarded-For", "1.2.3.4"); // real client

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }
  }

  @Nested
  @MockitoSettings(strictness = Strictness.LENIENT)
  class PresharedSecret {

    @BeforeEach
    void setUpApiTokenJws() {
      when(jwtTokenService.verifyToken(anyString())).thenReturn(jws);
      when(jws.getHeader()).thenReturn(jwsHeader);
      when(jws.getPayload()).thenReturn(claims);
      when(jwsHeader.getType()).thenReturn(SessionTokenType.API.name());
      when(appTokenProperties.getAllowedIpAddresses()).thenReturn(Set.of());
      // Claims needs entrySet() for Map.copyOf() and untyped get() for createPrincipal()
      when(claims.entrySet()).thenReturn(Set.of());
      when(claims.get(JwtTokenService.CLAIM_ROLES)).thenReturn(List.of());
      when(claims.get(JwtTokenService.CLAIM_RIGHTS)).thenReturn(List.of());
    }

    @Test
    void noPskConfigured_noHeaderRequired() {
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of());

      MockHttpServletRequest request = new MockHttpServletRequest();
      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void validPsk_passes() {
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of("secret123"));
      when(appTokenProperties.getPresharedSecretHeaderName()).thenReturn("X-API-Token-PSK");

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-API-Token-PSK", "secret123");

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void wrongPsk_throws() {
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of("secret123"));
      when(appTokenProperties.getPresharedSecretHeaderName()).thenReturn("X-API-Token-PSK");

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-API-Token-PSK", "wrong-secret");

      assertThatThrownBy(() -> filter.getAuthentication("token", request))
          .isInstanceOf(ApiTokenConstraintViolationAuthenticationException.class)
          .hasMessageContaining("Invalid preshared secret");
    }

    @Test
    void missingPskHeader_throws() {
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of("secret123"));
      when(appTokenProperties.getPresharedSecretHeaderName()).thenReturn("X-API-Token-PSK");

      MockHttpServletRequest request = new MockHttpServletRequest();
      assertThatThrownBy(() -> filter.getAuthentication("token", request))
          .isInstanceOf(ApiTokenConstraintViolationAuthenticationException.class)
          .hasMessageContaining("Invalid preshared secret");
    }

    @Test
    void nonApiToken_skipsPskCheck() {
      when(jwsHeader.getType()).thenReturn(SessionTokenType.ACCESS.name());
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of("secret123"));

      // No PSK header — but it's a session token, must pass
      MockHttpServletRequest request = new MockHttpServletRequest();
      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }

    @Test
    void multiplePsks_anyValidSecretPasses() {
      when(appTokenProperties.getPresharedSecrets()).thenReturn(Set.of("secret-a", "secret-b"));
      when(appTokenProperties.getPresharedSecretHeaderName()).thenReturn("X-API-Token-PSK");

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-API-Token-PSK", "secret-b");

      assertThatNoException().isThrownBy(() -> filter.getAuthentication("token", request));
    }
  }
}
