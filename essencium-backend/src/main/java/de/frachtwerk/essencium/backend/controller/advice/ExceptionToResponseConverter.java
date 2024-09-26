package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.ReportableException;
import de.frachtwerk.essencium.backend.model.exception.response.EssenciumExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public interface ExceptionToResponseConverter<
    E extends ReportableException, R extends EssenciumExceptionResponse> {

  R convert(E exception, HttpStatus httpStatus, HttpServletRequest request);
}
