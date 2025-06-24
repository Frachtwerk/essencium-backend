/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.User;
import de.frachtwerk.essencium.backend.model.dto.AppUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetailsImpl;
import de.frachtwerk.essencium.backend.model.representation.UserRepresentation;
import de.frachtwerk.essencium.backend.model.representation.assembler.UserAssembler;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.service.UserService;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
public class UserController
    extends AbstractUserController<
        User, UserRepresentation, AppUserDto, BaseUserSpec<User, Long>, Long> {

  protected UserController(UserService userService, UserAssembler assembler) {
    super(userService, assembler);
  }

  @Override
  public Page<UserRepresentation> findAll(
      BaseUserSpec<User, Long> specification, Pageable pageable) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    EssenciumUserDetailsImpl asd = (EssenciumUserDetailsImpl) authentication.getPrincipal();
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    authentication.getDetails();
    authentication.getName();
    authentication.isAuthenticated();
    authentication.getAuthorities();
    return super.findAll(specification, pageable);
  }
}
