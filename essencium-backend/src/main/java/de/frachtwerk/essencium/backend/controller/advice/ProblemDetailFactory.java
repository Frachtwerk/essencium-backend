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

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

  private static final String MESSAGE_PARAMETER = "message";
  private static final String GENERIC_ERROR_DETAIL = "An error occurred";
  private static final String TRACE_PARAMETER = "trace";
  private static final String STACK_TRACE_PROPERTY = "stackTrace";

  private final EssenciumErrorProperties errorProperties;
  private final WebProperties webProperties;

  public ProblemDetailFactory(
      EssenciumErrorProperties errorProperties, WebProperties webProperties) {
    this.errorProperties = errorProperties;
    this.webProperties = webProperties;
  }

  public ProblemDetail create(
      HttpStatus status,
      ErrorCode errorCode,
      String detail,
      Throwable throwable,
      HttpServletRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(status, resolveDetail(detail, request));
    problemDetail.setType(URI.create(errorProperties.getUrnPrefix() + errorCode.name()));
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", Instant.now().toString());

    if (shouldIncludeStackTrace(request)) {
      problemDetail.setProperty(STACK_TRACE_PROPERTY, getStackTrace(throwable));
    }

    return problemDetail;
  }

  private boolean shouldIncludeStackTrace(HttpServletRequest request) {
    ErrorProperties.IncludeAttribute includeStackTrace =
        webProperties.getError().getIncludeStacktrace();

    return switch (includeStackTrace) {
      case ALWAYS -> true;
      case NEVER -> false;
      case ON_PARAM -> isParameterEnabled(request, TRACE_PARAMETER);
    };
  }

  private String getStackTrace(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  private String resolveDetail(String detail, HttpServletRequest request) {
    if (shouldIncludeMessage(request)) {
      return detail;
    }

    return GENERIC_ERROR_DETAIL;
  }

  private boolean shouldIncludeMessage(HttpServletRequest request) {
    ErrorProperties.IncludeAttribute includeMessage = webProperties.getError().getIncludeMessage();

    return switch (includeMessage) {
      case ALWAYS -> true;
      case NEVER -> false;
      case ON_PARAM -> isParameterEnabled(request, MESSAGE_PARAMETER);
    };
  }

  private boolean isParameterEnabled(HttpServletRequest request, String parameterName) {
    String parameterValue = request.getParameter(parameterName);
    return parameterValue != null && !"false".equalsIgnoreCase(parameterValue);
  }
}
