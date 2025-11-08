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

package de.frachtwerk.essencium.backend.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.AppCorsProperties;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.filter.CorsFilter;

@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

  @Mock private AppCorsProperties appCorsProperties;

  @InjectMocks private CorsConfig corsConfig;

  @BeforeEach
  void setUp() {
    reset(appCorsProperties);
  }

  @Test
  void testCorsFilter_WithAllowedOriginPatterns() {
    List<String> originPatterns = List.of("https://*.example.com", "http://localhost:*");
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(originPatterns);
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(5)).getAllowedOriginPatterns();
    verify(appCorsProperties, times(2)).isAllowCredentials();
  }

  @Test
  void testCorsFilter_WithWildcardOriginPattern() {
    List<String> originPatterns = List.of("*");
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(originPatterns);
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(5)).getAllowedOriginPatterns();
  }

  @Test
  void testCorsFilter_WithAllowedOrigins_WhenNoOriginPatterns() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(null);
    List<String> origins = List.of("http://localhost:3000", "https://app.example.com");
    when(appCorsProperties.getAllowedOrigins()).thenReturn(origins);
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(4)).getAllowedOrigins();
  }

  @Test
  void testCorsFilter_WithEmptyOriginPatterns_FallbackToOrigins() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(new ArrayList<>());
    List<String> origins = List.of("http://localhost:3000");
    when(appCorsProperties.getAllowedOrigins()).thenReturn(origins);
    when(appCorsProperties.isAllowCredentials()).thenReturn(false);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("Content-Type"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(1800L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(4)).getAllowedOrigins();
  }

  @Test
  void testCorsFilter_WithNullOriginPatternsAndNullOrigins() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(null);
    when(appCorsProperties.getAllowedOrigins()).thenReturn(null);
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with null origins");
  }

  @Test
  void testCorsFilter_WithEmptyOriginPatternsAndEmptyOrigins() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(new ArrayList<>());
    when(appCorsProperties.getAllowedOrigins()).thenReturn(new ArrayList<>());
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with empty origins");
  }

  @Test
  void testCorsFilter_WithNullAllowedMethods() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(null);
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with null allowed methods");
  }

  @Test
  void testCorsFilter_WithEmptyAllowedMethods() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(new ArrayList<>());
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with empty allowed methods");
  }

  @Test
  void testCorsFilter_WithNullAllowedHeaders() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(null);
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with null allowed headers");
  }

  @Test
  void testCorsFilter_WithEmptyAllowedHeaders() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(new ArrayList<>());
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with empty allowed headers");
  }

  @Test
  void testCorsFilter_WithNullExposedHeaders() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(null);
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with null exposed headers");
  }

  @Test
  void testCorsFilter_WithEmptyExposedHeaders() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(new ArrayList<>());
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with empty exposed headers");
  }

  @Test
  void testCorsFilter_WithNullMaxAge() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(null);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter, "CorsFilter should be created even with null maxAge");
  }

  @Test
  void testCorsFilter_WithAllNullProperties() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(null);
    when(appCorsProperties.getAllowedOrigins()).thenReturn(null);
    when(appCorsProperties.isAllowCredentials()).thenReturn(false);
    when(appCorsProperties.getAllowedMethods()).thenReturn(null);
    when(appCorsProperties.getAllowedHeaders()).thenReturn(null);
    when(appCorsProperties.getExposedHeaders()).thenReturn(null);
    when(appCorsProperties.getMaxAge()).thenReturn(null);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(
        corsFilter, "CorsFilter should be created even when all properties are null/empty");
  }

  @Test
  void testCorsFilter_WithCredentialsDisabled() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(false);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(2)).isAllowCredentials();
  }

  @Test
  void testCorsFilter_WithCustomMaxAge() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(7200L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(2)).getMaxAge();
  }

  @Test
  void testCorsFilter_WithMultipleAllowedHeaders() {
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(List.of("*"));
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST", "PUT", "DELETE"));
    when(appCorsProperties.getAllowedHeaders())
        .thenReturn(List.of("Content-Type", "Authorization", "X-Requested-With"));
    when(appCorsProperties.getExposedHeaders())
        .thenReturn(List.of("Authorization", "X-Custom-Header"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(3)).getAllowedHeaders();
    verify(appCorsProperties, times(3)).getExposedHeaders();
  }

  @Test
  void testCorsFilter_OriginPatternsTakePrecedenceOverOrigins() {
    List<String> originPatterns = List.of("https://*.example.com");
    when(appCorsProperties.getAllowedOriginPatterns()).thenReturn(originPatterns);
    when(appCorsProperties.isAllowCredentials()).thenReturn(true);
    when(appCorsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST"));
    when(appCorsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
    when(appCorsProperties.getExposedHeaders()).thenReturn(List.of("Authorization"));
    when(appCorsProperties.getMaxAge()).thenReturn(3600L);

    CorsFilter corsFilter = corsConfig.corsFilter();

    assertNotNull(corsFilter);
    verify(appCorsProperties, times(5)).getAllowedOriginPatterns();
    verify(appCorsProperties, never()).getAllowedOrigins();
  }
}
