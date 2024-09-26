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
