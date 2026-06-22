package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

    private static final String MESSAGE_PARAMETER = "message";
    private static final String GENERIC_ERROR_DETAIL = "An error occurred";

    private final EssenciumErrorProperties errorProperties;
    private final WebProperties webProperties;

    public ProblemDetailFactory(EssenciumErrorProperties errorProperties, WebProperties webProperties) {
        this.errorProperties = errorProperties;
        this.webProperties = webProperties;
    }

    public ProblemDetail create(
            HttpStatus status, ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(status, resolveDetail(detail, request));

        problemDetail.setType(URI.create(errorProperties.getUrnPrefix() + errorCode.name()));
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }

    private String resolveDetail(String detail, HttpServletRequest request) {
        if (shouldIncludeMessage(request)) {
            return detail;
        }

        return GENERIC_ERROR_DETAIL;
    }

    private boolean shouldIncludeMessage(HttpServletRequest request) {
        ErrorProperties.IncludeAttribute includeMessage = webProperties.getError().getIncludeMessage();

        return switch (includeMessage) {
            case ALWAYS -> true;
            case NEVER -> false;
            case ON_PARAM -> isParameterEnabled(request, MESSAGE_PARAMETER);
        };
    }

    private boolean isParameterEnabled(HttpServletRequest request, String parameterName) {
        String parameterValue = request.getParameter(parameterName);
        return parameterValue != null && !"false".equalsIgnoreCase(parameterValue);
    }
}