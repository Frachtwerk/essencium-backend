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

package de.frachtwerk.essencium.backend.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoggingConfig implements WebMvcConfigurer {

  private static final String REQUEST_IDENTIFIER_KEY = "Request-Identifier";

  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(new LoggingInterceptor());
  }

  private static class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
        final @NotNull HttpServletRequest request,
        final @NotNull HttpServletResponse response,
        final @NotNull Object handler) {
      var requestId = UUID.randomUUID().toString();
      MDC.put(REQUEST_IDENTIFIER_KEY, requestId);
      return true;
    }

    @Override
    public void afterCompletion(
        final @NotNull HttpServletRequest request,
        final @NotNull HttpServletResponse response,
        final @NotNull Object handler,
        final Exception ex) {
      MDC.remove(REQUEST_IDENTIFIER_KEY);
    }
  }
}
