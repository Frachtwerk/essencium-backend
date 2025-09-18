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

package de.frachtwerk.essencium.backend.api.data.user;

import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UserDtoBuilder {

  UserDtoBuilder() {}

  private String password;
  private String email = "test.user@frachtwerk.de";
  private String source;
  private Set<String> roles = new HashSet<>();

  public UserDtoBuilder withEmail(String email) {
    this.email = email;
    return this;
  }

  public UserDtoBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  public UserDtoBuilder withRoles(String... roles) {
    this.roles.clear();
    this.roles.addAll(Arrays.asList(roles));
    return this;
  }

  public UserDtoBuilder withSource(String source) {
    this.source = source;

    return this;
  }

  public UserDto<Long> buildDefaultUserDto() {
    UserDto<Long> userDto = new UserDto<>();

    userDto.setId(1L);
    userDto.setEmail(this.email);
    userDto.setRoles(this.roles);
    userDto.setPassword(this.password);
    userDto.setSource(this.source);

    return userDto;
  }
}
