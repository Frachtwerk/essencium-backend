package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

    private final EssenciumErrorProperties errorProperties;

    public ProblemDetailFactory(EssenciumErrorProperties errorProperties) {
        this.errorProperties = errorProperties;
    }

    public ProblemDetail create(
            HttpStatus status, ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setType(URI.create(errorProperties.getUrnPrefix() + errorCode.name()));
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }
}