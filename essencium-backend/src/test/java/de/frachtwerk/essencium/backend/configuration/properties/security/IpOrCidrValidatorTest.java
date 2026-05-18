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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IpOrCidrValidatorTest {

  private final IpOrCidrValidator validator = new IpOrCidrValidator();

  static Stream<Arguments> cases() {
    return Stream.of(
        // --- valid inputs ---
        Arguments.of("IPv4 address", "203.0.113.42", true),
        Arguments.of("IPv4 CIDR /8", "10.0.0.0/8", true),
        Arguments.of("IPv4 CIDR /32", "192.168.1.1/32", true),
        Arguments.of("IPv4 with leading space", " 1.2.3.4", true),
        Arguments.of("IPv6 address", "2001:db8::1", true),
        Arguments.of("IPv6 CIDR /32", "2001:db8::/32", true),
        Arguments.of("loopback IPv4", "127.0.0.1", true),
        Arguments.of("loopback IPv6", "::1", true),
        Arguments.of("loopback IPv6", "::/32", true),

        // --- invalid inputs ---
        Arguments.of("null", null, false),
        Arguments.of("empty string", "", false),
        Arguments.of("blank string", "   ", false),
        Arguments.of("plain hostname", "example.com", false),
        Arguments.of("random text", "not-an-ip", false),
        Arguments.of("IPv4 octet out of range", "999.999.999.999", false),
        Arguments.of("CIDR prefix too large", "192.168.0.0/33", false),
        Arguments.of("partial IPv4", "192.168.1", false),
        Arguments.of("IPv4 with port", "192.168.1.1:8080", false),
        Arguments.of("IPv6 with port", "[2001:db8::1]:8080", false));
  }

  @ParameterizedTest(name = "{0}: \"{1}\" → {2}")
  @MethodSource("cases")
  void validate(String description, String input, boolean expectedValid) {
    assertThat(validator.isValid(input, null)).isEqualTo(expectedValid);
  }
}
