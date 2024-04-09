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
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

  private final WebProperties.Resources resourceProperties;

  @Autowired
  public StaticResourceConfig(WebProperties resourceProperties) {
    this.resourceProperties = resourceProperties.getResources();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations(resourceProperties.getStaticLocations())
        .resourceChain(true)
        .addResolver(new FallbackResourceResolver("index.html"));
  }

  private static class FallbackResourceResolver extends PathResourceResolver {
    private final String fallbackPath;

    private FallbackResourceResolver(String fallbackPath) {
      this.fallbackPath = fallbackPath;
    }

    @Override
    public Resource resolveResource(
        HttpServletRequest request,
        @NotNull String requestPath,
        @NotNull List<? extends Resource> locations,
        @NotNull ResourceResolverChain chain) {
      final Resource resolvedResource =
          super.resolveResource(request, requestPath, locations, chain);
      return resolvedResource != null && resolvedResource.exists()
          ? resolvedResource
          : super.resolveResource(request, fallbackPath, locations, chain);
    }
  }
}
