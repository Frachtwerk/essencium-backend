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

import de.frachtwerk.essencium.backend.controller.advice.*;
import de.frachtwerk.essencium.backend.model.exception.ReportableException;
import de.frachtwerk.essencium.backend.model.exception.response.EssenciumExceptionResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExceptionReportingConfig {

  @Bean
  public GlobalExceptionHandler controllerAdvice() {
    return new GlobalExceptionHandlerImp(exceptionToResponseConverter(), exceptionToStatusMapper());
  }

  @Bean
  public ExceptionToResponseConverter<ReportableException, EssenciumExceptionResponse>
      exceptionToResponseConverter() {
    return new ExceptionToResponseConverterImp();
  }

  @Bean
  public ExceptionToStatusMapper exceptionToStatusMapper() {
    return new ExceptionToStatusMapperImp();
  }
}
