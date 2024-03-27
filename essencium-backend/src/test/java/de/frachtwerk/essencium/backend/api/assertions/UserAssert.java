package de.frachtwerk.essencium.backend.api.assertions;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.Role;
import java.util.Locale;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;

public class UserAssert extends AbstractAssert<UserAssert, UserStub> {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";

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

    public UserAssertAdditions andHasNoPasswordNorPasswordResetToken() {
      if (actual.getPassword() != null) {
        failWithMessage("The actual password is not null");
      }

      return andHasNoPasswordResetToken();
    }

    public UserAssertAdditions andHasNoPasswordResetToken() {
      if (actual.getPasswordResetToken() != null) {
        failWithMessage("The actual password reset token is not null");
      }

      return this;
    }

    public UserAssertAdditions andHasAValidPasswordResetToken() {
      if (actual.getPasswordResetToken() == null) {
        failWithMessage("The actual password reset token is null");
      }
      if (actual.getPasswordResetToken().isBlank()) {
        failWithMessage("The actual password reset token is blank");
      }
      if (!Pattern.matches(UUID_REGEX, actual.getPasswordResetToken())) {
        failWithMessage("The actual password reset token is not in a valid UUID format");
      }

      return this;
    }

    public UserAssertAdditions andHasFirstName(String expectedFirstName) {
      if (!actual.getFirstName().equals(expectedFirstName)) {
        failWithActualExpectedAndMessage(
            actual.getFirstName(),
            expectedFirstName,
            "The actual first name differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasNonce(String expectedNonce) {
      if (!actual.getNonce().equals(expectedNonce)) {
        failWithActualExpectedAndMessage(
            actual.getNonce(), expectedNonce, "The actual nonce differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasNotNonce(String expectedNonce) {
      if (actual.getNonce().equals(expectedNonce)) {
        failWithActualExpectedAndMessage(
            actual.getNonce(), expectedNonce, "The actual nonce does not differ from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasId(Long expectedId) {
      if (!actual.getId().equals(expectedId)) {
        failWithActualExpectedAndMessage(
            actual.getId(), expectedId, "The actual id differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasLastName(String expectedLastName) {
      if (!actual.getLastName().equals(expectedLastName)) {
        failWithActualExpectedAndMessage(
            actual.getLastName(), expectedLastName, "The actual last name differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasPhone(String expectedPhone) {
      if (!actual.getPhone().equals(expectedPhone)) {
        failWithActualExpectedAndMessage(
            actual.getPhone(), expectedPhone, "The actual phone differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasMobile(String expectedMobile) {
      if (!actual.getMobile().equals(expectedMobile)) {
        failWithActualExpectedAndMessage(
            actual.getMobile(), expectedMobile, "The actual mobile differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasLocale(Locale expectedLocale) {
      if (!actual.getLocale().equals(expectedLocale)) {
        failWithActualExpectedAndMessage(
            actual.getLocale(), expectedLocale, "The actual locale differs from the actual");
      }

      return this;
    }

    public UserAssertAdditions andHasNoRoles() {
      if (!actual.getRoles().isEmpty()) {
        failWithMessage("The actual roles are not empty");
      }

      return this;
    }

    public UserAssertAdditions andCanLogin() {
      if (actual.isLoginDisabled()) {
        failWithMessage("The actual login is disabled");
      }

      return this;
    }
  }
}
