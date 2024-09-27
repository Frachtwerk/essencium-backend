package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.controller.advice.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExceptionReportingConfig {

  @Bean
  public GlobalExceptionHandlerImp controllerAdvice() {
    return new GlobalExceptionHandlerImp(exceptionToResponseConverter(), exceptionToStatusMapper());
  }

  @Bean
  public ExceptionToResponseConverter exceptionToResponseConverter() {
    return new ExceptionToResponseConverterImp();
  }

  @Bean
  public ExceptionToStatusMapper exceptionToStatusMapper() {
    return new ExceptionToStatusMapperImp();
  }
}
