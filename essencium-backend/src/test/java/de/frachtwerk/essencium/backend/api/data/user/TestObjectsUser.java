package de.frachtwerk.essencium.backend.api.data.user;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;

public class TestObjectsUser {

  public AbstractBaseUser<Long> defaultUser() {
    UserStub baseUser = new UserStub();
    baseUser.setId(123L);
    baseUser.setEmail("base@user.de");

    return baseUser;
  }

  public UserDtoBuilder userDtoBuilder() {
    return new UserDtoBuilder();
  }
}
