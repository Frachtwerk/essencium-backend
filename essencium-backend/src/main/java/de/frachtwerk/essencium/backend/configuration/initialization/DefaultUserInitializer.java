/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static de.frachtwerk.essencium.backend.configuration.initialization.DefaultRoleInitializer.ADMIN_ROLE_NAME;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.RoleService;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultUserInitializer<
        USER extends AbstractBaseUser<ID>, USERDTO extends UserDto<ID>, ID extends Serializable>
    implements DataInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUserInitializer.class);

  @Value("${essencium-backend.initial-admin.username:devnull@frachtwerk.de}")
  public String ADMIN_USERNAME = "devnull@frachtwerk.de";

  @Value("${essencium-backend.initial-admin.password:adminAdminAdmin}")
  private String ADMIN_PASSWORD = "adminAdminAdmin";

  @Value("${essencium-backend.initial-admin.first-name:Admin}")
  public String ADMIN_FIRST_NAME = "Admin";

  @Value("${essencium-backend.initial-admin.last-name:User}")
  public String ADMIN_LAST_NAME = "User";

  private final RoleService roleService;
  private final AbstractUserService<USER, ID, USERDTO> userService;

  @Autowired
  public DefaultUserInitializer(
      @NotNull final RoleService roleService,
      @NotNull final AbstractUserService<USER, ID, USERDTO> userService) {
    this.roleService = roleService;
    this.userService = userService;
  }

  private Role getAdminRole() {
    final Role adminRole;

    try {
      adminRole = roleService.getRole(ADMIN_ROLE_NAME).orElseThrow(ResourceNotFoundException::new);
    } catch (ResourceNotFoundException ex) {
      throw new IllegalStateException("Admin role is not persistent!");
    }
    return adminRole;
  }

  private void createNewAdminUser() {
    final Role adminRole = getAdminRole();
    var adminUser = userService.getNewUser();
    adminUser.setEnabled(true);
    adminUser.setEmail(ADMIN_USERNAME);
    adminUser.setPassword(ADMIN_PASSWORD);
    adminUser.setFirstName(ADMIN_FIRST_NAME);
    adminUser.setLastName(ADMIN_LAST_NAME);
    adminUser.setRole(adminRole.getName());
    userService.create(adminUser);
    LOGGER.info("Default Admin user created [{}]", ADMIN_USERNAME);
  }

  @Override
  public void run() {
    if (userService.loadUsersByRole(ADMIN_ROLE_NAME).isEmpty()) {
      createNewAdminUser();
    }
  }

  @Override
  public int order() {
    return 40;
  }
}
