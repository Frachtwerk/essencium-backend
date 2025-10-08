/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumInitProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DefaultUserInitializer<
        USER extends AbstractBaseUser<ID>,
        AUTHUSER extends EssenciumUserDetails<ID>,
        USERDTO extends BaseUserDto<ID>,
        ID extends Serializable>
    implements DataInitializer {
  private static final String USER_FIELD_USERNAME = "username";
  private static final String USER_FIELD_PASSWORD = "password";
  private static final String USER_FIELD_FIRST_NAME = "firstName";
  private static final String USER_FIELD_LAST_NAME = "lastName";
  private static final String USER_FIELD_ROLES = "roles";
  private static final String[] MINIMUM_REQUIRED_USER_FIELDS = {
    USER_FIELD_USERNAME, USER_FIELD_ROLES, USER_FIELD_FIRST_NAME, USER_FIELD_LAST_NAME
  };

  private final AbstractUserService<USER, AUTHUSER, ID, USERDTO> userService;
  private final EssenciumInitProperties essenciumInitProperties;

  @Override
  public int order() {
    return 40;
  }

  @Override
  public void run() {
    Set<Map<String, Object>> users = essenciumInitProperties.getUsers();

    Set<Map<String, Object>> sanitizedUsers = getSanitizedUsers(users);

    validateUsersMap(sanitizedUsers);

    for (Map<String, Object> user : sanitizedUsers) {
      String username = (String) user.get(USER_FIELD_USERNAME);
      userService
          .findByEmailIgnoreCase(username)
          .ifPresentOrElse(
              existingUser -> updateExistingUser(user, existingUser), () -> createNewUser(user));
    }
  }

  private static Set<Map<String, Object>> getSanitizedUsers(Set<Map<String, Object>> users) {
    // Rebuild userMap
    Set<Map<String, Object>> sanitizedUsers = new HashSet<>();
    for (Map<String, Object> user : users) {
      Map<String, Object> sanitizedUser = new HashMap<>(user);
      // Sanitize hyphen notation
      for (Map.Entry<String, Object> entry : user.entrySet()) {
        String key = entry.getKey();
        if (key.contains("-")) {
          String camelCaseKey =
              Arrays.stream(key.split("-"))
                  .map(s -> s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1))
                  .collect(Collectors.joining());
          camelCaseKey = camelCaseKey.substring(0, 1).toLowerCase() + camelCaseKey.substring(1);
          if (!user.containsKey(camelCaseKey)) {
            sanitizedUser.put(camelCaseKey, user.get(key));
            sanitizedUser.remove(key);
          }
        }
      }
      sanitizedUsers.add(sanitizedUser);
    }
    return sanitizedUsers;
  }

  private void validateUsersMap(Set<Map<String, Object>> users) {
    for (String field : MINIMUM_REQUIRED_USER_FIELDS) {
      for (Map<String, Object> user : users) {
        if (!user.containsKey(field)) {
          throw new IllegalArgumentException(
              "User configuration is missing required field: " + field);
        } else {
          validateFieldTypes(field, user);
        }
      }
    }
  }

  private static void validateFieldTypes(String field, Map<String, Object> user) {
    switch (field) {
      case USER_FIELD_FIRST_NAME,
          USER_FIELD_LAST_NAME,
          USER_FIELD_USERNAME,
          USER_FIELD_PASSWORD -> {
        if (!(user.get(field) instanceof String)) {
          throw new IllegalArgumentException(field + " must be a string.");
        }
      }
      case USER_FIELD_ROLES -> {
        Object rolesObj = user.get(field);
        boolean valid =
            (rolesObj instanceof Collection<?> collection
                    && collection.stream().allMatch(String.class::isInstance))
                || (rolesObj instanceof Map<?, ?> map
                    && map.values().stream().allMatch(String.class::isInstance));
        if (!valid) {
          throw new IllegalArgumentException(
              "User roles must be a collection of strings or a map with string values.");
        }
      }
      default ->
          log.info(
              "Found field {} in user configuration, no validation implemented. Field is of type {}.",
              field,
              user.get(field) != null ? user.get(field).getClass().getName() : "null");
    }
  }

  private void updateExistingUser(Map<String, Object> userProperties, USER user) {
    HashSet<String> roles =
        user.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(HashSet::new));

    Set<String> propertyRoles = getRoles(userProperties);
    roles.addAll(propertyRoles);

    Map<String, Object> updates = new HashMap<>();
    updates.put(USER_FIELD_ROLES, roles);
    for (Map.Entry<String, Object> entry : userProperties.entrySet()) {
      if (entry.getKey().equals(USER_FIELD_ROLES)
          || entry.getKey().equals(USER_FIELD_USERNAME)
          || entry.getKey().equals(USER_FIELD_PASSWORD)) {
        continue; // already handled
      }
      updates.put(entry.getKey(), entry.getValue());
    }

    userService.patch(user.getId(), updates);
    log.info("Updated user with id {}", user.getId());
  }

  private void createNewUser(Map<String, Object> userProperties) {
    USERDTO user = userService.getNewUser();
    // set default fields
    user.setEmail((String) userProperties.get(USER_FIELD_USERNAME));
    user.setRoles(getRoles(userProperties));
    if (userProperties.containsKey(USER_FIELD_PASSWORD)) {
      user.setPassword((String) userProperties.get(USER_FIELD_PASSWORD));
    }

    Set<String> processedFields =
        Set.of(USER_FIELD_USERNAME, USER_FIELD_ROLES, USER_FIELD_PASSWORD);

    // get updatable fiels from user
    Arrays.stream(user.getClass().getMethods())
        .filter(method -> method.getName().startsWith("set"))
        .filter(method -> method.getParameterCount() == 1)
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .forEach(
            method -> {
              String fieldName =
                  method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);

              if (userProperties.containsKey(fieldName) && !processedFields.contains(fieldName)) {
                try {
                  method.invoke(user, userProperties.get(fieldName));
                } catch (Exception e) {
                  log.warn("Could not set field {} on user: {}", fieldName, e.getMessage());
                }
              }
            });

    USER createdUser = userService.create(user);
    log.info("Created user with id {}", createdUser.getId());
  }

  private static Set<String> getRoles(Map<String, Object> userProperties) {
    return switch (userProperties.get(USER_FIELD_ROLES)) {
      case Map<?, ?> mapRoles ->
          mapRoles.values().stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .collect(Collectors.toSet());
      case Collection<?> collectionRoles ->
          collectionRoles.stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .collect(Collectors.toSet());
      default -> Collections.emptySet();
    };
  }
}
