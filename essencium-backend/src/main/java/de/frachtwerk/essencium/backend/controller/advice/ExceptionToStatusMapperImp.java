/*
 *
 *  * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *  *
 *  * This file is part of essencium-backend.
 *  *
 *  * essencium-backend is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * essencium-backend is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.frachtwerk.essencium.backend.controller.advice;

import de.frachtwerk.essencium.backend.model.exception.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class ExceptionToStatusMapperImp implements ExceptionToStatusMapper {
  protected final Map<Class<? extends Exception>, HttpStatus> exceptionMap = new HashMap<>();

  public ExceptionToStatusMapperImp() {
    // General exceptions
    exceptionMap.put(RuntimeException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR);

    // Essencium exceptions
    exceptionMap.put(ResourceCannotFindException.class, HttpStatus.NOT_FOUND);
    exceptionMap.put(ResourceException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(DuplicateResourceException.class, HttpStatus.CONFLICT);
    exceptionMap.put(InvalidInputException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(MissingDataException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(NotAllowedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(TranslationFileException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(AuthenticationTokenException.class, HttpStatus.UNAUTHORIZED);

    // Spring exceptions
    exceptionMap.put(AuthenticationException.class, HttpStatus.UNAUTHORIZED);
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
