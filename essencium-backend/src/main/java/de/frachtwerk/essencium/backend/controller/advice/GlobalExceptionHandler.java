package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.model.exception.TokenInvalidationException;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return createResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInputException(
            InvalidInputException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceUpdateException.class)
    public ResponseEntity<ProblemDetail> handleResourceUpdateException(
            ResourceUpdateException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT, exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
            DuplicateResourceException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, exception.getMessage(), request);
    }

    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleNotAllowedException(
            NotAllowedException exception, HttpServletRequest request) {
        return createResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(TokenInvalidationException.class)
    public ResponseEntity<ProblemDetail> handleTokenInvalidationException(
            TokenInvalidationException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALIDATION, exception.getMessage(), request);
    }

    @ExceptionHandler(TranslationFileException.class)
    public ResponseEntity<ProblemDetail> handleTranslationFileException(
            TranslationFileException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.TRANSLATION_FILE_ERROR,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                        .toList();

        ProblemDetail problemDetail =
                problemDetailFactory.create(
                        HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Validation failed", request);

        problemDetail.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors =
                exception.getParameterValidationResults().stream().flatMap(this::toFieldErrors).toList();

        ProblemDetail problemDetail =
                problemDetailFactory.create(
                        HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Validation failed", request);

        problemDetail.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                "Malformed request body",
                request);
    }

    private ResponseEntity<ProblemDetail> createResponse(
            HttpStatus status, ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = problemDetailFactory.create(status, errorCode, detail, request);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private Stream<FieldErrorResponse> toFieldErrors(ParameterValidationResult validationResult) {
        String field = validationResult.getMethodParameter().getParameterName();

        return validationResult.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .map(message -> new FieldErrorResponse(field, message));
    }
}