/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.reset-credentials-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(
    name = "ResetCredentialsController",
    description =
        "Set of endpoints used to reset a user's credentials, given a valid reset token as previously received via email")
public class ResetCredentialsController<
    USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>> {

  private final AbstractUserService<USER, ID, USERDTO> userService;
  private final Random random;

  @Autowired
  ResetCredentialsController(@NotNull final AbstractUserService<USER, ID, USERDTO> userService) {
    this.userService = userService;
    random = new Random();
  }

  @PostMapping("/reset-credentials")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(description = "Request a new reset token via email")
  public void requestResetToken(@RequestBody @NotNull final String username) {
    // to prevent user enumeration, we add a random delay between 0,8s and 3s.
    try {
      Thread.sleep(random.nextInt(800, 3000));
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
    userService.createResetPasswordToken(username);
  }

  @PostMapping("/set-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(description = "Update the password of the user, who the given reset token belongs to")
  public void setNewPassword(@RequestBody @NotNull final PasswordUpdateRequest setPasswordRequest) {
    userService.resetPasswordByToken(
        setPasswordRequest.verification(), setPasswordRequest.password());
  }

  /**
   * to prevent a user enumeration vulnerability, a non-existing user generates a status 204
   * response as well. additionally, we add more 800 ms delay.
   */
  @ExceptionHandler({UsernameNotFoundException.class})
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void handleUsernameNotFoundException() {
    try {
      Thread.sleep(800);
    } catch (InterruptedException ignored) {
    }
    // don't return anything, only send 204 status
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS);
  }
}
