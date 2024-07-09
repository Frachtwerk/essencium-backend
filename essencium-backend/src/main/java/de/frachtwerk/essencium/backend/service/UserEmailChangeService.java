package de.frachtwerk.essencium.backend.service;

import static java.lang.String.format;

import de.frachtwerk.essencium.backend.configuration.properties.SecurityConfigProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.EmailVerificationRequest;
import de.frachtwerk.essencium.backend.model.exception.DuplicateResourceException;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.security.BruteForceProtectionService;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserEmailChangeService<USER extends AbstractBaseUser<ID>, ID extends Serializable> {

  private static final Logger LOG = LoggerFactory.getLogger(UserEmailChangeService.class);

  private final BaseUserRepository<USER, ID> userRepository;

  private final UserMailService userMailService;

  private final BruteForceProtectionService<USER, ID> bruteForceProtectionService;

  private final SecurityConfigProperties securityConfigProperties;

  @Autowired
  public UserEmailChangeService(
      @NotNull final BaseUserRepository<USER, ID> userRepository,
      @NotNull final UserMailService userMailService,
      @NotNull BruteForceProtectionService<USER, ID> bruteForceProtectionService,
      SecurityConfigProperties securityConfigProperties) {
    this.userRepository = userRepository;
    this.userMailService = userMailService;
    this.bruteForceProtectionService = bruteForceProtectionService;
    this.securityConfigProperties = securityConfigProperties;
  }

  public void startEmailVerificationProcessIfNeededForAndTrackDuplication(
      @NotNull USER user, @NotNull Optional<String> optionalNewEmail)
      throws DuplicateResourceException {
    if (isEmailVerificationDisabled(user)) {
      return;
    }
    startEmailVerificationProcessIfNeededFor(user, optionalNewEmail, true);
  }

  public void startEmailVerificationProcessIfNeededFor(
      @NotNull USER user, @NotNull Optional<String> optionalNewEmail)
      throws DuplicateResourceException {
    if (isEmailVerificationDisabled(user)) {
      return;
    }
    startEmailVerificationProcessIfNeededFor(user, optionalNewEmail, false);
  }

  public void verifyEmailByToken(@NotNull final EmailVerificationRequest emailVerificationRequest)
      throws DuplicateResourceException {
    if (securityConfigProperties.isEMailValidationDisabled()) {
      LOG.warn("Try to verify email by a token, but the email verification is disabled");
      return;
    }
    var userToUpdate =
        userRepository
            .findByEmailVerifyToken(emailVerificationRequest.emailVerifyToken())
            .orElseThrow(() -> new BadCredentialsException("Invalid verification token"));

    if (LocalDateTime.now().isAfter(userToUpdate.getEmailVerificationTokenExpiringAt())) {
      throw new BadCredentialsException("Verification token expired");
    }

    userToUpdate.verifyEmail();

    String anonymizedEmail = anonymizedEmail(userToUpdate.getEmail());

    LOG.info("Changed email for user to {}", anonymizedEmail);

    userRepository.save(userToUpdate);
  }

  public boolean validateEmailChange(
      @NotNull USER user, @NotNull String newEmail, boolean trackDuplication)
      throws DuplicateResourceException {
    if (isEmailVerificationDisabled(user)) {
      return false;
    }

    if (!newEmail.isBlank() && !user.getEmail().equalsIgnoreCase(newEmail)) {

      if (!user.hasLocalAuthentication()) {
        throw new NotAllowedException(
            format("Cannot change email for users authenticated via '%s'", user.getSource()));
      }

      if (user.getLastRequestedEmailChange() != null
          && user.getLastRequestedEmailChange()
              .isAfter(
                  LocalDateTime.now()
                      .minusMinutes(securityConfigProperties.getEMailUpdateIntervallInMinutes()))) {
        throw new NotAllowedException(
            format(
                "Changing the email is only every %s minutes possible",
                securityConfigProperties.getEMailUpdateIntervallInMinutes()));
      }

      checkIfEmailAlreadyExists(user, newEmail, trackDuplication);

      return true;
    }

    return false;
  }

  private boolean isEmailVerificationDisabled(USER user) {
    if (securityConfigProperties.isEMailValidationDisabled()) {
      LOG.info("Skip email verification for user {} since it is disabled", user.getId());
      return true;
    }
    return false;
  }

  private void startEmailVerificationProcessIfNeededFor(
      USER user, Optional<String> optionalNewEmail, boolean trackDuplication)
      throws DuplicateResourceException {
    optionalNewEmail.ifPresent(
        newEmail -> {
          boolean emailVerificationNeeded = validateEmailChange(user, newEmail, trackDuplication);

          if (emailVerificationNeeded) {
            createAndSendVerificationTokenMail(user, newEmail);
          }
        });
  }

  private void createAndSendVerificationTokenMail(USER user, String newEmail) {
    user.generateVerifyEmailStateFor(
        newEmail, securityConfigProperties.getEMailTokenValidityInMinutes());
    try {
      userMailService.sendVerificationMail(newEmail, user.getEmailVerifyToken(), user.getLocale());
    } catch (CheckedMailException e) {
      LOG.error("Failed to send email verification to: {}", e.getLocalizedMessage());
    }
  }

  private void checkIfEmailAlreadyExists(USER user, String newEmail, boolean trackDuplication)
      throws DuplicateResourceException {

    boolean emailAlreadyExists = userRepository.existsByEmailIgnoreCase(newEmail);

    String anonymizedEmail = anonymizedEmail(newEmail);

    if (emailAlreadyExists) {
      if (trackDuplication) {
        LOG.info("Tried to set email to the already existing email {}", anonymizedEmail);

        bruteForceProtectionService.registerLoginFailure(user.getUsername());
      }

      throw new DuplicateResourceException("Email already exists");
    }
  }

  private String anonymizedEmail(String newEmail) {
    String anonymizedEmail = newEmail;
    String[] emailSplit = newEmail.split("@");

    if (emailSplit.length == 2) {
      anonymizedEmail = format("***@%s", emailSplit[1]);
    }
    return anonymizedEmail;
  }
}
