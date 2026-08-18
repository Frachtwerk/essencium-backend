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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a {@link ProblemDetail} straight to the response for errors raised in the security filter
 * chain, which never reaches the exception advices. Without it those responses fall through to
 * {@code sendError} and Boot's {@code /error} dispatch, whose body has a different shape.
 */
@Component
public class ProblemDetailWriter {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailWriter.class);

  private final ProblemDetailFactory problemDetailFactory;
  private final ObjectMapper objectMapper;

  public ProblemDetailWriter(ProblemDetailFactory problemDetailFactory, ObjectMapper objectMapper) {
    this.problemDetailFactory = problemDetailFactory;
    this.objectMapper = objectMapper;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatusCode status,
      ProblemErrorCode errorCode,
      @Nullable String detail,
      @Nullable Throwable throwable)
      throws IOException {
    if (response.isCommitted()) {
      log.debug("Response already committed, unable to write a problem detail");
      return;
    }

    ProblemDetail problemDetail =
        problemDetailFactory.create(status, errorCode, detail, throwable, request);

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), problemDetail);
    response.flushBuffer();
  }
}
