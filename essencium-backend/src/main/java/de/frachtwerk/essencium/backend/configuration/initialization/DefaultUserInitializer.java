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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.configuration.properties.InitProperties;
import de.frachtwerk.essencium.backend.configuration.properties.UserProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DefaultUserInitializer<
        USER extends AbstractBaseUser<ID>, USERDTO extends UserDto<ID>, ID extends Serializable>
    implements DataInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUserInitializer.class);

  private final AbstractUserService<USER, ID, USERDTO> userService;
  private final InitProperties initProperties;

  @Override
  public int order() {
    return 40;
  }

  @Override
  public void run() {
    List<USER> existingUsers = userService.getAll();
    initProperties
        .getUsers()
        .forEach(
            userProperties ->
                existingUsers.stream()
                    .filter(user -> user.getEmail().equals(userProperties.getUsername()))
                    .findAny()
                    .ifPresentOrElse(
                        user -> updateExistingUser(userProperties, user),
                        () -> createNewUser(userProperties)));
  }

  private void updateExistingUser(UserProperties userProperties, USER user) {
    HashSet<String> roles =
        user.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(HashSet::new));
    roles.addAll(userProperties.getRoles());

    userService.patch(user.getId(), Map.of("roles", roles));

    LOGGER.info("Updated user with id {}", user.getId());
  }

  private void createNewUser(UserProperties userProperties) {
    USERDTO user = userService.getNewUser();
    user.setEmail(userProperties.getUsername());
    user.setFirstName(userProperties.getFirstName());
    user.setLastName(userProperties.getLastName());
    user.setRoles(userProperties.getRoles());

    if (Objects.nonNull(userProperties.getPassword())) {
      user.setPassword(userProperties.getPassword());
    }

    USER createdUser = userService.create(user);
    LOGGER.info("Created user with id {}", createdUser.getId());
  }
}
