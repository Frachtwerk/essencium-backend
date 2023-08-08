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

package de.frachtwerk.essencium.backend.controller;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.service.ContactMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/contact")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.contact-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(name = "ContactController", description = "Set of endpoints to send arbitrary emails")
public class ContactController {

  private final ContactMailService contactService;

  public ContactController(@NotNull final ContactMailService contactService) {
    this.contactService = contactService;
  }

  @PostMapping
  @Operation(description = "As a logged in user, send a certain message to the given address")
  public void sendContactRequest(
      @RequestBody @NotNull final ContactRequestDto contactRequest,
      @Parameter(hidden = true) @AuthenticationPrincipal final AbstractBaseUser user)
      throws CheckedMailException {

    contactService.sendContactRequest(contactRequest, user);
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS);
  }
}
