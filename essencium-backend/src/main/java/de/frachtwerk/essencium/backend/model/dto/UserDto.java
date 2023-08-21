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

package de.frachtwerk.essencium.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.frachtwerk.essencium.backend.model.validation.StrongPassword;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserDto<ID extends Serializable> {
  public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  @Nullable private ID id;

  @Builder.Default private boolean enabled = true;

  private String email;

  @NotEmpty private String firstName;

  @NotEmpty private String lastName;

  private String phone;

  private String mobile;

  @Nullable
  @StrongPassword(allowEmpty = true) // for non-local users, empty password is given
  @Builder.Default
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password = "";

  @JsonIgnore private String passwordResetToken;

  @NotNull @Builder.Default private Locale locale = UserDto.DEFAULT_LOCALE;

  private String role;

  @JsonIgnore private String source;

  @Builder.Default private boolean loginDisabled = false;
}
