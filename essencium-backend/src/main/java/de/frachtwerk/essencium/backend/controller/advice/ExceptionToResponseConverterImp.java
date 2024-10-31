/*
 *
 *  * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import de.frachtwerk.essencium.backend.model.exception.ReportableException;
import de.frachtwerk.essencium.backend.model.exception.response.EssenciumExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExceptionToResponseConverterImp
    implements ExceptionToResponseConverter<ReportableException, EssenciumExceptionResponse> {

  @Value("${exceptions.response.level:base}")
  private String exceptionsResponseLevel;

  @Override
  public final EssenciumExceptionResponse convert(
      ReportableException exception, HttpStatus status, HttpServletRequest request) {
    EssenciumExceptionResponse response = new EssenciumExceptionResponse();
    response.setStatus(status.value());
    response.setError(status.getReasonPhrase());
    response.setPath(request.getRequestURI());
    response.setTimestamp(LocalDateTime.now());

    if (exceptionsResponseLevel.equals("internal") || exceptionsResponseLevel.equals("debug")) {
      response.setInternal(exception.reportInternals());
    }

    if (exceptionsResponseLevel.equals("debug")) {
      response.setDebug(exception.reportDebug());
    }

    return response;
  }
}
