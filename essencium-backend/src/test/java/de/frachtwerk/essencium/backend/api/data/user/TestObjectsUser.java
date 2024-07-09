package de.frachtwerk.essencium.backend.api.data.user;

import de.frachtwerk.essencium.backend.configuration.properties.SecurityConfigProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class TestObjectsUser {

  public static final long TEST_USER_ID = 4711133742L;
  public static final String TEST_USERNAME = "test.user@frachtwerk.de";
  public static final String TEST_FIRST_NAME = "TEST_FIRST_NAME";
  public static final String TEST_LAST_NAME = "TEST_LAST_NAME";
  public static final String TEST_PHONE = "TEST_PHONE";
  public static final String TEST_MOBILE = "TEST_MOBILE";
  public static final boolean TEST_LOGIN_DISABLED = false;
  public static final String TEST_PASSWORD_PLAIN = "frachtwerk";
  public static final String TEST_PASSWORD_HASH =
      "{bcrypt}$2b$10$dwJpN2XigdXZLvviA4dIkOuQC31/8JdgD60o5uCYGT.OBn1WDtL9i";
  public static final String TEST_PASSWORD_RESET_TOKEN = UUID.randomUUID().toString();
  public static final String TEST_NONCE = "78fd553y";
  public static final Locale TEST_LOCALE = Locale.GERMAN;
  public static final String TEST_SOURCE = AbstractBaseUser.USER_AUTH_SOURCE_LOCAL;
  public static final String TEST_NEW_EMAIL = "new@email.de";

  private final SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();

  public UserStub internal() {
    return UserStub.builder()
        .id(TEST_USER_ID)
        .email(TEST_USERNAME)
        .enabled(true)
        .firstName(TEST_FIRST_NAME)
        .lastName(TEST_LAST_NAME)
        .locale(TEST_LOCALE)
        .password(TEST_PASSWORD_HASH)
        .passwordResetToken(null)
        .nonce(TEST_NONCE)
        .source(TEST_SOURCE)
        .loginDisabled(TEST_LOGIN_DISABLED)
        .build();
  }

  public UserStub external() {
    UserStub userStub = internal();

    userStub.setSource(AbstractBaseUser.USER_AUTH_SOURCE_LDAP);
    userStub.setPassword(null);

    return userStub;
  }

  public UserStub passwordReset() {
    UserStub userStub = internal();

    userStub.setLoginDisabled(true);
    userStub.setPasswordResetToken(TEST_PASSWORD_RESET_TOKEN);

    return userStub;
  }

  public UserStub validateEmail() {
    UserStub userStub = internal();

    userStub.setEmailToVerify(TEST_NEW_EMAIL);
    userStub.setEmailVerifyToken(UUID.randomUUID());
    userStub.setEmailVerificationTokenExpiringAt(
        LocalDateTime.now().plusMinutes(securityConfigProperties.getEMailTokenValidityInMinutes()));
    userStub.setLastRequestedEmailChange(LocalDateTime.now());

    return userStub;
  }

  public UserDtoBuilder userDtoBuilder() {
    return new UserDtoBuilder();
  }

  public UserDto<Long> defaultUserUpdateDto() {
    final String NEW_FIRST_NAME = "Robin";
    final String NEW_LAST_NAME = "The Ripper";
    final String NEW_PHONE = "018012345";
    final String NEW_MOBILE = "018098765";
    final Locale NEW_LOCALE = Locale.ITALY;
    final String NEW_PASSWORD = "hopefully not working!";

    final UserDto<Long> updates = new UserDto<>();
    updates.setFirstName(NEW_FIRST_NAME);
    updates.setLastName(NEW_LAST_NAME);
    updates.setPhone(NEW_PHONE);
    updates.setMobile(NEW_MOBILE);
    updates.setLocale(NEW_LOCALE);
    updates.setPassword(NEW_PASSWORD);

    return updates;
  }

  public UserDto<Long> newEmailUserUpdateDto() {
    UserDto<Long> userDto = defaultUserUpdateDto();

    userDto.setEmail(TEST_NEW_EMAIL);

    return userDto;
  }
}
