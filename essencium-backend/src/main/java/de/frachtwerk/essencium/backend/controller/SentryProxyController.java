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

import de.frachtwerk.essencium.backend.configuration.properties.SentryConfigProperties;
import de.frachtwerk.essencium.backend.model.Feedback;
import de.frachtwerk.essencium.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sentry")
@ConditionalOnProperty(
    value = "essencium-backend.overrides.sentry-proxy-controller",
    havingValue = "false",
    matchIfMissing = true)
@Tag(
    name = "SentryProxyController",
    description =
        "Endpoints to proxy unauthenticated requests from the client to a configured Sentry instance")
public class SentryProxyController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryProxyController.class);
  @NotNull private final FeedbackService feedbackService;

  @Autowired
  public SentryProxyController(
      SentryConfigProperties sentryConfigProperties, @NotNull FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
    if (!sentryConfigProperties.isValid()) {
      LOGGER.warn(
          "Sentry configuration is invalid as one or more properties are missing. Sentry reporting, tracing or feedback might not work.");
    }
  }

  @PostMapping("/feedback")
  @Operation(description = "Sends to Sentry a user feedback request")
  public Feedback sendFeedback(@RequestBody Feedback feedback) {
    feedbackService.sendFeedback(feedback);
    return feedback;
  }

  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public final ResponseEntity<?> collectionOptions() {
    return ResponseEntity.ok().allow(getAllowedMethods().toArray(new HttpMethod[0])).build();
  }

  protected Set<HttpMethod> getAllowedMethods() {
    return Set.of(HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS);
  }
}
