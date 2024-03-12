package de.frachtwerk.essencium.backend.api.assertions;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.Role;
import org.assertj.core.api.AbstractAssert;

public class UserAssert extends AbstractAssert<UserAssert, UserStub> {

  protected UserAssert(UserStub actual) {
    super(actual, UserAssert.class);
  }

  public UserAssertAdditions isNonNull() {
    if (actual == null) {
      failWithMessage("The given user is null");
    }

    return new UserAssertAdditions(actual);
  }

  public class UserAssertAdditions {

    private final UserStub actual;

    public UserAssertAdditions(UserStub actual) {
      this.actual = actual;
    }

    public UserAssertAdditions andHasEmail(String expectedEmail) {
      if (!actual.getEmail().equals(expectedEmail)) {
        failWithActualExpectedAndMessage(
            actual.getEmail(), expectedEmail, "The expected email differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasSource(String expectedSource) {
      if (!actual.getSource().equals(expectedSource)) {
        failWithActualExpectedAndMessage(
            actual.getSource(), expectedSource, "The expected source differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasOnlyTheRoles(Role... roles) {
      andHasAtLeastTheRoles(roles);

      if (actual.getRoles().size() != roles.length) {
        failWithActualExpectedAndMessage(
            actual.getRoles().size(),
            roles.length,
            "The user has more roles than the expected roles");
      }

      return this;
    }

    public UserAssertAdditions andHasAtLeastTheRoles(Role... roles) {
      for (Role role : roles) {
        if (!actual.getRoles().contains(role)) {
          failWithActualExpectedAndMessage(
              actual.getRoles(), role, "The user has not the expected role");
        }
      }

      return this;
    }

    public UserAssertAdditions andHasPassword(String expectedPassword) {
      if (!actual.getPassword().equals(expectedPassword)) {
        failWithActualExpectedAndMessage(
            actual.getPassword(),
            expectedPassword,
            "The expected password differs from the actual");
      }

      return this;
    }
  }
}
