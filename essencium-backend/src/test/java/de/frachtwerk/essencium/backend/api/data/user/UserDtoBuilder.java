package de.frachtwerk.essencium.backend.api.data.user;

import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.TEST_SOURCE;
import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.TEST_USERNAME;
import static de.frachtwerk.essencium.backend.api.data.user.TestObjectsUser.TEST_USER_ID;

import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UserDtoBuilder {

  UserDtoBuilder() {}

  private Long id = TEST_USER_ID;
  private String password;
  private String email = TEST_USERNAME;
  private String source = TEST_SOURCE;
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

  public UserDtoBuilder withSource(String source) {
    this.source = source;

    return this;
  }

  public UserDto<Long> buildDefaultUserDto() {
    UserDto<Long> userDto = new UserDto<>();

    userDto.setId(this.id);
    userDto.setEmail(this.email);
    userDto.setRoles(this.roles);
    userDto.setPassword(this.password);
    userDto.setSource(this.source);

    return userDto;
  }
}
