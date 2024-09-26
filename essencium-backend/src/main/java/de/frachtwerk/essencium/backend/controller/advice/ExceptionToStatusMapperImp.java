package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExceptionToStatusMapperImp implements ExceptionToStatusMapper {
  private final Map<Class<? extends Exception>, HttpStatus> exceptionMap = new HashMap<>();

  public ExceptionToStatusMapperImp() {
    exceptionMap.put(RuntimeException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(ResourceCannotFindException.class, HttpStatus.NOT_FOUND);
    exceptionMap.put(ResourceException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(DuplicateResourceException.class, HttpStatus.CONFLICT);
    exceptionMap.put(InvalidInputException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(MissingDataException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(NotAllowedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(TranslationFileException.class, HttpStatus.BAD_REQUEST);
  }

  public HttpStatus map(Exception exception) {
    return mapExceptionHierarchy(exception.getClass());
  }

  private HttpStatus mapExceptionHierarchy(Class<?> clazz) {
    if (exceptionMap.containsKey(clazz)) {
      return exceptionMap.get(clazz);
    } else if (clazz.getSuperclass() != null) {
      return mapExceptionHierarchy(clazz.getSuperclass());
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR; // Default fallback
    }
  }
}
