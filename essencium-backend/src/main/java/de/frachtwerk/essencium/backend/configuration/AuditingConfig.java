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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
public class AuditingConfig<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    implements AuditorAware<String> {

  AuditingConfig() {}

  @NotNull
  @Override
  public Optional<String> getCurrentAuditor() {
    var context = Optional.ofNullable(SecurityContextHolder.getContext());

    return context
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getPrincipal)
        .filter(it -> AbstractBaseUser.class.isAssignableFrom(it.getClass()))
        .map(it -> (USER) it)
        .map(USER::getEmail);
  }
}
