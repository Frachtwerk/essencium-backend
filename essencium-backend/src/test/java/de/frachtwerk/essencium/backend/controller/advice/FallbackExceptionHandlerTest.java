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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class FallbackExceptionHandlerTest {

  @Mock private ProblemDetailFactory problemDetailFactory;

  @InjectMocks private FallbackExceptionHandler exceptionHandler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest("GET", "/v1/test");

    lenient()
        .when(problemDetailFactory.create(any(), any(), anyString(), any(), any()))
        .thenAnswer(
            invocation ->
                ProblemDetail.forStatusAndDetail(
                    invocation.getArgument(0), invocation.getArgument(2)));
  }

  @Test
  void returnsInternalServerErrorProblemDetail() {
    Exception exception = new IllegalStateException("boom");

    ResponseEntity<ProblemDetail> response =
        exceptionHandler.handleUnhandledException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verify(problemDetailFactory)
        .create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR,
            "Internal server error",
            exception,
            request);
  }
}
