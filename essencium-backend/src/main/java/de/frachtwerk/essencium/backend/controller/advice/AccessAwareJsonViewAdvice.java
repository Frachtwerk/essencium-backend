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

package de.frachtwerk.essencium.backend.controller.advice;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import de.frachtwerk.essencium.backend.controller.access.AccessAwareJsonFilter;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

@RestControllerAdvice
public class AccessAwareJsonViewAdvice extends AbstractMappingJacksonResponseBodyAdvice {
  public static final String FILTER_NAME = "roleBasedFilter";

  @Override
  protected void beforeBodyWriteInternal(
      @NotNull MappingJacksonValue bodyContainer,
      @NotNull MediaType contentType,
      @NotNull MethodParameter returnType,
      @NotNull ServerHttpRequest request,
      @NotNull ServerHttpResponse response) {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      final var principal =
          (AbstractBaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      if (principal != null && principal.getRole() != null) {
        FilterProvider filters =
            new SimpleFilterProvider()
                .addFilter(FILTER_NAME, new AccessAwareJsonFilter<>(principal));
        bodyContainer.setFilters(filters);
      }
    }
  }
}
