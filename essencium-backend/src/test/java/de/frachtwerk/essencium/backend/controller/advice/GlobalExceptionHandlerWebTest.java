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

package de.frachtwerk.essencium.backend.controller.advice;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Checks which handler an exception actually reaches, which the mock-based test cannot. */
class GlobalExceptionHandlerWebTest {

  private static final String URN = "urn:frachtwerk:error:";

  @RestController
  static class ProbeController {

    @GetMapping("/probe/denied")
    public String denied() {
      throw new AccessDeniedException("Access is denied");
    }

    @GetMapping("/probe/typed/{id}")
    public String typed(@PathVariable Long id) {
      return "ok " + id;
    }

    @GetMapping("/probe/param")
    public String param(@RequestParam String required) {
      return required;
    }

    @GetMapping("/probe/cookie")
    public String cookie(@CookieValue("SESSION") String session) {
      return session;
    }

    @GetMapping("/probe/response-status")
    public String responseStatus() {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "internal reason 4711");
    }

    @GetMapping("/probe/response-status-no-reason")
    public String responseStatusWithoutReason() {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/probe/response-status-unauthorized")
    public String responseStatusUnauthorized() {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
    }

    @GetMapping("/probe/response-status-forbidden")
    public String responseStatusForbidden() {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource");
    }

    @GetMapping("/probe/annotated")
    public String annotated() {
      throw new AnnotatedConflictException("Already exists");
    }

    @GetMapping("/probe/boom")
    public String boom() {
      throw new IllegalStateException("internal reason 4711");
    }

    @GetMapping("/probe/wrapped-denied")
    public String wrappedDenied() {
      throw new IllegalStateException("wrapper", new AccessDeniedException("Access is denied"));
    }

    @GetMapping("/probe/wrapped-not-found")
    public String wrappedNotFound() {
      throw new IllegalStateException("wrapper", new ResourceNotFoundException("Resource missing"));
    }

    @PostMapping(value = "/probe/body", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String body(@Valid @RequestBody Payload payload) {
      return payload.name();
    }
  }

  record Payload(@NotBlank String name) {}

  @ResponseStatus(HttpStatus.CONFLICT)
  static class AnnotatedConflictException extends RuntimeException {
    AnnotatedConflictException(String message) {
      super(message);
    }
  }

  private static MockMvc mockMvc(ErrorProperties.IncludeAttribute includeMessage) {
    return mockMvc(includeMessage, includeMessage);
  }

  private static MockMvc mockMvc(
      ErrorProperties.IncludeAttribute includeMessage,
      ErrorProperties.IncludeAttribute includeBindingErrors) {
    WebProperties webProperties = new WebProperties();
    webProperties.getError().setIncludeMessage(includeMessage);
    webProperties.getError().setIncludeBindingErrors(includeBindingErrors);

    ProblemDetailFactory factory =
        new ProblemDetailFactory(new EssenciumErrorProperties(), webProperties);

    return MockMvcBuilders.standaloneSetup(new ProbeController())
        .setControllerAdvice(
            new GlobalExceptionHandler(factory), new FallbackExceptionHandler(factory))
        .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("framework exceptions keep their status")
  class FrameworkExceptions {

    private final MockMvc mockMvc = mockMvc(ErrorProperties.IncludeAttribute.ALWAYS);

    @Test
    void accessDeniedIsForbiddenForAnAuthenticatedCaller() throws Exception {
      SecurityContextHolder.getContext()
          .setAuthentication(new TestingAuthenticationToken("user", "n/a", "READ"));

      mockMvc
          .perform(get("/probe/denied"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.type").value(URN + "FORBIDDEN"));
    }

    @Test
    void accessDeniedIsUnauthorizedForAnAnonymousCaller() throws Exception {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new AnonymousAuthenticationToken(
                  "key", "anonymous", AuthorityUtils.createAuthorityList("ANONYMOUS")));

      mockMvc
          .perform(get("/probe/denied"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.type").value(URN + "AUTHENTICATION_FAILED"));
    }

    @Test
    void typeMismatchIsBadRequest() throws Exception {
      mockMvc
          .perform(get("/probe/typed/abc"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value(URN + "INVALID_INPUT"));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
      mockMvc
          .perform(post("/probe/typed/1"))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(jsonPath("$.type").value(URN + "METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeIsUnsupportedMediaType() throws Exception {
      mockMvc
          .perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("nope"))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.type").value(URN + "UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void missingRequestParameterIsBadRequest() throws Exception {
      mockMvc.perform(get("/probe/param")).andExpect(status().isBadRequest());
    }

    @Test
    void missingCookieIsBadRequest() throws Exception {
      mockMvc.perform(get("/probe/cookie")).andExpect(status().isBadRequest());
    }

    @Test
    void responseStatusExceptionKeepsItsStatus() throws Exception {
      mockMvc
          .perform(get("/probe/response-status"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.type").value(URN + "NOT_FOUND"))
          .andExpect(jsonPath("$.detail").value("internal reason 4711"));
    }

    @Test
    void malformedBodyIsBadRequestWithMalformedRequestCode() throws Exception {
      mockMvc
          .perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value(URN + "MALFORMED_REQUEST"));
    }

    @Test
    void validationFailureCarriesFieldErrors() throws Exception {
      mockMvc
          .perform(
              post("/probe/body")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value(URN + "VALIDATION_FAILED"))
          .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    @Test
    void wrappedAccessDeniedStillResolvesToItsHandler() throws Exception {
      SecurityContextHolder.getContext()
          .setAuthentication(new TestingAuthenticationToken("user", "n/a", "READ"));

      mockMvc
          .perform(get("/probe/wrapped-denied"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.type").value(URN + "FORBIDDEN"));
    }

    @Test
    void wrappedDomainExceptionStillResolvesToItsHandler() throws Exception {
      mockMvc
          .perform(get("/probe/wrapped-not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.type").value(URN + "NOT_FOUND"));
    }

    @Test
    void responseStatusExceptionReportsTheCodeMatchingItsStatus() throws Exception {
      mockMvc
          .perform(get("/probe/response-status-unauthorized"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.type").value(URN + "AUTHENTICATION_FAILED"));

      mockMvc
          .perform(get("/probe/response-status-forbidden"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.type").value(URN + "FORBIDDEN"));
    }

    @Test
    void responseStatusExceptionWithoutReasonDoesNotLeakTheStatusPrefix() throws Exception {
      mockMvc
          .perform(get("/probe/response-status-no-reason"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("An error occurred"));
    }

    @Test
    void annotatedExceptionKeepsItsDeclaredStatus() throws Exception {
      mockMvc
          .perform(get("/probe/annotated"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.type").value(URN + "CONFLICT"))
          .andExpect(jsonPath("$.detail").value("Already exists"));
    }

    @Test
    void unmappedExceptionIsInternalServerError() throws Exception {
      mockMvc
          .perform(get("/probe/boom"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.type").value(URN + "INTERNAL_SERVER_ERROR"));
    }
  }

  @Nested
  @DisplayName("include-message=never")
  class MessagesSuppressed {

    private final MockMvc mockMvc = mockMvc(ErrorProperties.IncludeAttribute.NEVER);

    @Test
    void responseStatusExceptionDoesNotLeakItsReason() throws Exception {
      mockMvc
          .perform(get("/probe/response-status"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("An error occurred"));
    }

    @Test
    void missingCookieDoesNotLeakTheFrameworkMessage() throws Exception {
      mockMvc
          .perform(get("/probe/cookie"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("An error occurred"));
    }

    @Test
    void unmappedExceptionDoesNotLeakItsMessage() throws Exception {
      mockMvc
          .perform(get("/probe/boom"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.detail").value("An error occurred"));
    }

    @Test
    void fieldErrorsFollowTheirOwnSettingRatherThanTheMessageSetting() throws Exception {
      mockMvc(ErrorProperties.IncludeAttribute.NEVER, ErrorProperties.IncludeAttribute.ALWAYS)
          .perform(
              post("/probe/body")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("An error occurred"))
          .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    @Test
    void validationFailureDoesNotExposeFieldErrors() throws Exception {
      mockMvc
          .perform(
              post("/probe/body")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
  }
}
