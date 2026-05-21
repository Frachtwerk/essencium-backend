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

package de.frachtwerk.essencium.backend.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

public class IpOrCidrValidator implements ConstraintValidator<ValidIpOrCidr, String> {

  private static final Pattern IPV4 =
      Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(/\\d{1,3})?$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    String host = trimmed.contains("/") ? trimmed.substring(0, trimmed.indexOf('/')) : trimmed;

    // Structural pre-check: rejects hostnames and malformed strings without a DNS lookup.
    // IPv6 addresses contain ':', IPv4 addresses must have exactly four dot-separated parts.
    if (!looksLikeIpAddress(host)) {
      return false;
    }

    // Verify the host parses as a valid InetAddress (catches out-of-range octets etc.)
    try {
      InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      return false;
    }

    // Delegate full CIDR expression validation (prefix length, address family, etc.)
    try {
      new IpAddressMatcher(trimmed);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean looksLikeIpAddress(String host) {
    return host.contains(":") || IPV4.matcher(host).matches();
  }
}
