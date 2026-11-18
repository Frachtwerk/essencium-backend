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

package de.frachtwerk.essencium.backend.configuration;

/**
 * Extends or overrides the default access rules of {@link EssenciumEndpointAccess}. Register one or
 * more beans of this type; they are applied in {@link org.springframework.core.annotation.Order}
 * after the Essencium defaults, and the result is used for both the security configuration and the
 * OpenAPI documentation.
 *
 * <pre>{@code
 * @Bean
 * EndpointAccessCustomizer v2Access() {
 *   return rules -> rules.protect("/v2/**").permit(HttpMethod.GET, "/v2/status");
 * }
 * }</pre>
 */
@FunctionalInterface
public interface EndpointAccessCustomizer {

  void customize(EssenciumEndpointAccess.Builder rules);
}
