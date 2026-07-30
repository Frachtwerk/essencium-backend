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

package de.frachtwerk.essencium.backend.configuration.properties.embedded;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

/**
 * A single mapping from an external identity-provider group/role to an Essencium role.
 *
 * <p>Used in the {@code roles} lists of the OAuth2 and LDAP configurations ({@link
 * de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties}, {@link
 * de.frachtwerk.essencium.backend.configuration.properties.auth.AppLdapProperties} and the
 * per-provider overrides in {@code OAuth2ClientRegistrationProperties}). During login the values
 * read from the provider's role claim/attribute are matched against {@link #src}; on a match the
 * user is granted the Essencium role named by {@link #dst}. Both fields are mandatory ({@link
 * NotEmpty}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class UserRoleMapping {

  /**
   * Source value as delivered by the identity provider (e.g. an LDAP group or OAuth2 group claim).
   */
  @NotNull @NotEmpty private String src;

  /** Target Essencium role name the {@link #src} value is mapped to. */
  @NotNull @NotEmpty
  // @Pattern(regexp = "^[A-Z_]+$") // TODO: introduce validation for role mapping some day
  private String dst;
}
