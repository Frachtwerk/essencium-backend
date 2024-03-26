package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.EmailVerificationRequest;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
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

  private final BaseUserRepository<USER, ID> userRepository;

  private final UserMailService userMailService;

  @Autowired
  public UserEmailChangeService(
      @NotNull final BaseUserRepository<USER, ID> userRepository,
      @NotNull final UserMailService userMailService) {
    this.userRepository = userRepository;
    this.userMailService = userMailService;
  }

  public void createAndSendEmailVerificationTokenIfNeeded(
      @NotNull USER user, @NotNull Optional<String> optionalNewEmail) {

    if (optionalNewEmail.isPresent()) {
      String newEmail = optionalNewEmail.get();
      if (!newEmail.isBlank() && !user.getEmail().equals(newEmail)) {

        if (!user.hasLocalAuthentication()) {
          throw new NotAllowedException(
              String.format(
                  "cannot change email for users authenticated via '%s'", user.getSource()));
        }

        user.generateVerifyEmailStateFor(newEmail, E_MAIL_TOKEN_VALIDITY_IN_MONTHS);

        try {

          userMailService.sendVerificationMail(
              newEmail, user.getEmailVerifyToken(), user.getLocale());

        } catch (CheckedMailException e) {
          LOG.error("Failed to send email verification to: {}", e.getLocalizedMessage());
        }
      }
    }
  }

  public void verifyEmailByToken(@NotNull final EmailVerificationRequest emailVerificationRequest) {
    var userToUpdate =
        userRepository
            .findByEmailVerifyToken(emailVerificationRequest.emailVerifyToken())
            .orElseThrow(() -> new BadCredentialsException("Invalid verification token"));

    if (LocalDateTime.now().isAfter(userToUpdate.getEmailVerificationTokenExpiringAt())) {
      throw new BadCredentialsException("Verification token expired");
    }

    userToUpdate.verifyEmail();

    userRepository.save(userToUpdate);
  }
}
