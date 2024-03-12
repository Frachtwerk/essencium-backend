package de.frachtwerk.essencium.backend.api.data.user;

import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UserDtoBuilder {

  UserDtoBuilder() {}

  private String password;
  private String email = "test.user@frachtwerk.de";

  private Set<String> roles = new HashSet<>();

  public UserDtoBuilder withEmail(String email) {
    this.email = email;
    return this;
  }

  public UserDtoBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  public UserDtoBuilder withRoles(String... roles) {
    this.roles.clear();
    this.roles.addAll(Arrays.asList(roles));
    return this;
  }

  public UserDto<Long> buildLongUserDto() {
    UserDto<Long> userDto = new UserDto<>();

    userDto.setEmail(this.email);
    userDto.setRoles(this.roles);
    userDto.setPassword(this.password);

    return userDto;
  }
}
