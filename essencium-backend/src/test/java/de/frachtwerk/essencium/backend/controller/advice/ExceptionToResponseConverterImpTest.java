/*
 *
 *  * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *  *
 *  * This file is part of essencium-backend.
 *  *
 *  * essencium-backend is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * essencium-backend is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.frachtwerk.essencium.backend.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.exception.ReportableException;
import de.frachtwerk.essencium.backend.model.exception.response.EssenciumExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExceptionToResponseConverterImpTest {

  private ExceptionToResponseConverterImp sut;

  private ReportableException exception;
  private HttpServletRequest request;
  private HttpStatus httpStatus;

  @BeforeEach
  void setUp() {
    sut = new ExceptionToResponseConverterImp();

    exception = mock(ReportableException.class);
    request = mock(HttpServletRequest.class);
    httpStatus = HttpStatus.BAD_REQUEST;

    when(request.getRequestURI()).thenReturn("/test-uri");
  }

  @Test
  @DisplayName("base level")
  void convertBase() {
    setReportLevel("base");

    EssenciumExceptionResponse response = sut.convert(exception, httpStatus, request);

    assertResponseBaseAttributes(response);
    assertThat(response.getInternal()).isNull();
    assertThat(response.getDebug()).isNull();
  }

  @Test
  @DisplayName("internal level")
  void convertInternal() {
    setUpInternalResponse();
    setReportLevel("internal");

    EssenciumExceptionResponse response = sut.convert(exception, httpStatus, request);

    assertResponseBaseAttributes(response);
    assertThat(response.getInternal()).isNotNull();
    assertThat(response.getInternal().get("internals")).isEqualTo("internals");
    assertThat(response.getDebug()).isNull();
  }

  @Test
  @DisplayName("debug level")
  void convertDebug() {
    setUpDebugResponse();
    setReportLevel("debug");

    EssenciumExceptionResponse response = sut.convert(exception, httpStatus, request);

    assertResponseBaseAttributes(response);

    assertThat(response.getInternal()).isNotNull();
    assertThat(response.getInternal().get("internals")).isEqualTo("internals");
    assertThat(response.getDebug()).isNotNull();
    assertThat(response.getDebug().get("debug")).isEqualTo("debug");
  }

  private void setUpInternalResponse() {
    when(exception.reportInternals()).thenReturn(Map.of("internals", "internals"));
  }

  private void setUpDebugResponse() {
    setUpInternalResponse();
    when(exception.reportDebug()).thenReturn(Map.of("debug", "debug"));
  }

  private void setReportLevel(String level) {
    ReflectionTestUtils.setField(sut, "exceptionsResponseLevel", level);
  }

  private void assertResponseBaseAttributes(EssenciumExceptionResponse response) {
    assertNotNull(response);
    assertThat(response.getStatus()).isEqualTo(httpStatus.value());
    assertThat(response.getError()).isEqualTo(httpStatus.getReasonPhrase());
    assertThat(response.getPath()).isEqualTo("/test-uri");
  }
}
