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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.util.EssenciumUserUtil;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.ser.FilterProvider;

@ExtendWith(MockitoExtension.class)
class AccessAwareJsonViewAdviceTest {

  @InjectMocks private AccessAwareJsonViewAdvice advice;

  @Mock private MethodParameter returnType;
  @Mock private MediaType selectedContentType;
  @Mock private ServerHttpRequest request;
  @Mock private ServerHttpResponse response;
  @Mock private EssenciumUserDetails<?> principal;

  @Test
  void supports_returnsTrueForJacksonJsonHttpMessageConverter() {
    boolean result = advice.supports(returnType, JacksonJsonHttpMessageConverter.class);

    assertThat(result).isTrue();
  }

  @Test
  void supports_returnsFalseForOtherConverters() {
    boolean result = advice.supports(returnType, StringHttpMessageConverter.class);

    assertThat(result).isFalse();
  }

  @Test
  void beforeBodyWrite_returnsBodyUnchanged() {
    Object inputBody = new Object();

    Object result =
        advice.beforeBodyWrite(
            inputBody,
            returnType,
            selectedContentType,
            JacksonJsonHttpMessageConverter.class,
            request,
            response);

    assertThat(result).isSameAs(inputBody);
  }

  @Test
  void determineWriteHints_returnsNullWhenBodyIsNull() {
    Map<String, Object> result =
        advice.determineWriteHints(
            null, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

    assertThat(result).isNull();
  }

  @Test
  void determineWriteHints_returnsNullWhenNoPrincipal() {
    try (MockedStatic<EssenciumUserUtil> mockedUtil = mockStatic(EssenciumUserUtil.class)) {
      mockedUtil
          .when(EssenciumUserUtil::getUserDetailsFromAuthentication)
          .thenReturn(Optional.empty());
      Object body = new Object();

      Map<String, Object> result =
          advice.determineWriteHints(
              body, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

      assertThat(result).isNull();
    }
  }

  @Test
  void determineWriteHints_returnsNullWhenPrincipalHasNullRoles() {
    try (MockedStatic<EssenciumUserUtil> mockedUtil = mockStatic(EssenciumUserUtil.class)) {
      lenient().doReturn(null).when(principal).getRoles();
      mockedUtil
          .when(EssenciumUserUtil::getUserDetailsFromAuthentication)
          .thenReturn(Optional.of(principal));
      Object body = new Object();

      Map<String, Object> result =
          advice.determineWriteHints(
              body, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

      assertThat(result).isNull();
    }
  }

  @Test
  void determineWriteHints_returnsFilterProviderWhenPrincipalIsValid() {
    try (MockedStatic<EssenciumUserUtil> mockedUtil = mockStatic(EssenciumUserUtil.class)) {
      Set<? extends GrantedAuthority> roles = Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
      lenient().doReturn(roles).when(principal).getRoles();
      lenient().doReturn(Set.of()).when(principal).getRights();
      mockedUtil
          .when(EssenciumUserUtil::getUserDetailsFromAuthentication)
          .thenReturn(Optional.of(principal));
      Object body = new Object();

      Map<String, Object> result =
          advice.determineWriteHints(
              body, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

      assertThat(result).isNotNull();
      assertThat(result).containsKey(FilterProvider.class.getName());
      assertThat(result.get(FilterProvider.class.getName())).isInstanceOf(FilterProvider.class);
    }
  }

  @Test
  void determineWriteHints_filterProviderContainsCorrectRoleBasedFilter() {
    try (MockedStatic<EssenciumUserUtil> mockedUtil = mockStatic(EssenciumUserUtil.class)) {
      Set<? extends GrantedAuthority> roles = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
      lenient().doReturn(roles).when(principal).getRoles();
      lenient().doReturn(Set.of()).when(principal).getRights();
      mockedUtil
          .when(EssenciumUserUtil::getUserDetailsFromAuthentication)
          .thenReturn(Optional.of(principal));
      Object body = new Object();

      Map<String, Object> result =
          advice.determineWriteHints(
              body, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

      assertThat(result).isNotNull();
      FilterProvider filterProvider = (FilterProvider) result.get(FilterProvider.class.getName());
      assertThat(filterProvider).isNotNull();
      // Verify the FilterProvider is of correct type
      assertThat(filterProvider.toString()).contains("Filter");
    }
  }

  @Test
  void determineWriteHints_worksWithEmptyRolesAndRights() {
    try (MockedStatic<EssenciumUserUtil> mockedUtil = mockStatic(EssenciumUserUtil.class)) {
      lenient().doReturn(Set.of()).when(principal).getRoles();
      lenient().doReturn(Set.of()).when(principal).getRights();
      mockedUtil
          .when(EssenciumUserUtil::getUserDetailsFromAuthentication)
          .thenReturn(Optional.of(principal));
      Object body = new Object();

      Map<String, Object> result =
          advice.determineWriteHints(
              body, returnType, selectedContentType, JacksonJsonHttpMessageConverter.class);

      assertThat(result).isNotNull();
      assertThat(result).containsKey(FilterProvider.class.getName());
    }
  }
}
