package de.frachtwerk.essencium.backend.util;

import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {
  /**
   * Checks whether the current transaction is running within a user context.
   *
   * @return
   *     <ul>
   *       <li>empty {@link Optional} if invoked by any non-user-service (Scheduler, Initializer,
   *           Async-Jobs, ...)
   *       <li>{@link Optional< EssenciumUserDetails >} if current Transaction is executed in a User
   *           context
   *     </ul>
   */
  public static Optional<EssenciumUserDetails<? extends Serializable>>
      getUserDetailsFromAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.nonNull(authentication)
        && Objects.nonNull(authentication.getPrincipal())
        && authentication.getPrincipal() instanceof EssenciumUserDetails<?> userDetails) {
      return Optional.of(userDetails);
    }
    return Optional.empty();
  }

  public static HashSet<String> getRightsFromUserDetails(
      EssenciumUserDetails<? extends Serializable> userDetails) {
    return userDetails.getRights().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toCollection(HashSet::new));
  }
}
