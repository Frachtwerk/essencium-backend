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

package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import jakarta.annotation.Nonnull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class AccessAwareJsonViewAdvice implements ResponseBodyAdvice<Object> {
  public static final String FILTER_NAME = "roleBasedFilter";

  @Override
  public boolean supports(
      @Nonnull MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    // Support both Jackson2 (deprecated) and Jackson3 converters
    String converterName = converterType.getName();
    return converterName.contains("Jackson") && converterName.contains("HttpMessageConverter");
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      @Nonnull MethodParameter returnType,
      @Nonnull MediaType selectedContentType,
      @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @Nonnull ServerHttpRequest request,
      @Nonnull ServerHttpResponse response) {

    if (body == null || SecurityContextHolder.getContext().getAuthentication() == null) {
      return body;
    }

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication.getPrincipal() instanceof EssenciumUserDetails<?> principal)) {
      return body;
    }

    if (principal.getRoles() == null) {
      return body;
    }

    /*
    TODO: MappingJacksonValue is deprecated in Spring Boot 7.0 and causes wrapped responses
    For now, return the body directly without filtering
    A proper Jackson 3.x solution needs to be implemented using @JsonFilter on model classes
    */
    return body;
  }
}
