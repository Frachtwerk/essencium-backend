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

import de.frachtwerk.essencium.backend.controller.access.AccessAwareJsonFilter;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@RestControllerAdvice
public class AccessAwareJsonViewAdvice implements ResponseBodyAdvice<Object> {
  public static final String FILTER_NAME = "roleBasedFilter";

  private final ObjectMapper objectMapper;

  @Autowired
  public AccessAwareJsonViewAdvice(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(
      @Nonnull MethodParameter returnType,
      @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
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

    // Create FilterProvider with role-based filter (Jackson 3.x API)
    FilterProvider filters =
        new SimpleFilterProvider()
            .addFilter(FILTER_NAME, new AccessAwareJsonFilter<>(principal))
            .setFailOnUnknownId(false);

    // Configure a temporary ObjectMapper with the filter
    // In Jackson 3.x we use rebuild() and filterProvider()
    try {
      ObjectMapper filteredMapper = objectMapper.rebuild().filterProvider(filters).build();

      // Serialize and deserialize to apply the filters
      String json = filteredMapper.writeValueAsString(body);
      return filteredMapper.readValue(json, Object.class);
    } catch (Exception e) {
      // On error, return the original body
      return body;
    }
  }
}
