package de.frachtwerk.essencium.backend.model.exception.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EssenciumExceptionResponse {

  // Base
  private Integer status;
  private String error;
  private String path;
  private LocalDateTime timestamp;

  private Map<String, Object> internal;

  private Map<String, Object> debug;
}
