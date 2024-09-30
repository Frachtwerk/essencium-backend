package de.frachtwerk.essencium.backend.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public abstract class GlobalExceptionHandler extends ResponseEntityExceptionHandler {}
