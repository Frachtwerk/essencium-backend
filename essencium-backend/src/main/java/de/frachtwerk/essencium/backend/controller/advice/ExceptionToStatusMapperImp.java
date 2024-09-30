package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExceptionToStatusMapperImp implements ExceptionToStatusMapper {
  protected final Map<Class<? extends Exception>, HttpStatus> exceptionMap = new HashMap<>();

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

  private HttpStatus mapExceptionHierarchy(Class<?> exception) {
    if (exceptionMap.containsKey(exception)) {
      return exceptionMap.get(exception);
    } else if (exception.getSuperclass() != null) {
      return mapExceptionHierarchy(exception.getSuperclass());
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
