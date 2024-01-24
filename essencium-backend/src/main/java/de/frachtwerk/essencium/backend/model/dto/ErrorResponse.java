/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorProperties;

@Data
public class ErrorResponse {

  private final Integer status;
  private final String error;
  private final Object message;
  private final String timestamp;
  private final String path;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final Object errors;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Value("${server.error.include-message}")
  private ErrorProperties.IncludeAttribute includeMessageMode;

  public ErrorResponse(int status, Map<String, Object> errorAttributes) {
    this.status = status;
    this.error = errorAttributes.get("error").toString();
    this.message = includeMessage() ? errorAttributes.get("message") : "";
    this.errors = errorAttributes.get("errors");
    this.timestamp = errorAttributes.get("timestamp").toString();
    this.path = errorAttributes.get("path").toString();
  }

  private boolean includeMessage() {
    return includeMessageMode == null
        || includeMessageMode.equals(ErrorProperties.IncludeAttribute.ALWAYS);
  }
}
