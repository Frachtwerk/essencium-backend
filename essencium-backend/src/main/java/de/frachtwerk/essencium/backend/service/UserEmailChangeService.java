package de.frachtwerk.essencium.backend.service;

import static java.lang.String.format;

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

  public static final long E_MAIL_TOKEN_VALIDITY_IN_MONTHS = 1;
  public static final int E_MAIL_UPDATE_INTERVALL_IN_MINUTES = 30;

  private final BaseUserRepository<USER, ID> userRepository;

  private final UserMailService userMailService;

  private final BruteForceProtectionService<USER, ID> bruteForceProtectionService;

  @Autowired
  public UserEmailChangeService(
      @NotNull final BaseUserRepository<USER, ID> userRepository,
      @NotNull final UserMailService userMailService,
      @NotNull BruteForceProtectionService<USER, ID> bruteForceProtectionService) {
    this.userRepository = userRepository;
    this.userMailService = userMailService;
    this.bruteForceProtectionService = bruteForceProtectionService;
  }

  public void startEmailVerificationProcessIfNeededForAndTrackDuplication(
      @NotNull USER user, @NotNull Optional<String> optionalNewEmail)
      throws DuplicateResourceException {
    startEmailVerificationProcessIfNeededFor(user, optionalNewEmail, true);
  }

  public void startEmailVerificationProcessIfNeededFor(
      @NotNull USER user, @NotNull Optional<String> optionalNewEmail)
      throws DuplicateResourceException {
    startEmailVerificationProcessIfNeededFor(user, optionalNewEmail, false);
  }

  public void verifyEmailByToken(@NotNull final EmailVerificationRequest emailVerificationRequest)
      throws DuplicateResourceException {
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

  private void startEmailVerificationProcessIfNeededFor(
      USER user, Optional<String> optionalNewEmail, boolean trackDuplication)
      throws DuplicateResourceException {
    if (optionalNewEmail.isPresent()) {
      String newEmail = optionalNewEmail.get();
      if (!newEmail.isBlank() && !user.getEmail().equals(newEmail)) {

        if (!user.hasLocalAuthentication()) {
          throw new NotAllowedException(
              format("Cannot change email for users authenticated via '%s'", user.getSource()));
        }

        if (user.getLastRequestedEmailChange() != null
            && user.getLastRequestedEmailChange()
                .isAfter(LocalDateTime.now().minusMinutes(E_MAIL_UPDATE_INTERVALL_IN_MINUTES))) {
          throw new NotAllowedException(
              format(
                  "Changing the email is only every %s minutes possible",
                  E_MAIL_UPDATE_INTERVALL_IN_MINUTES));
        }

        checkIfEmailAlreadyExists(user, newEmail, trackDuplication);
        user.generateVerifyEmailStateFor(newEmail, E_MAIL_TOKEN_VALIDITY_IN_MONTHS);
        sendVerificationMail(user, newEmail);
      }
    }
  }

  private void sendVerificationMail(USER user, String newEmail) {
    try {
      userMailService.sendVerificationMail(newEmail, user.getEmailVerifyToken(), user.getLocale());
    } catch (CheckedMailException e) {
      LOG.error("Failed to send email verification to: {}", e.getLocalizedMessage());
    }
  }

  private void checkIfEmailAlreadyExists(USER user, String newEmail, boolean trackDuplication)
      throws DuplicateResourceException {
    boolean emailAlreadyExists =
        userRepository.exists(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.in(root.get("email")).value(newEmail));

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
