package de.frachtwerk.essencium.backend.controller.advice;

import org.springframework.http.HttpStatus;

public interface ExceptionToStatusMapper {

  HttpStatus map(Exception exception);
}
