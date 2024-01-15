/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.configuration.properties.MailConfigProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.model.mail.LoginMessageData;
import de.frachtwerk.essencium.backend.model.mail.ResetTokenMessageData;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import freemarker.template.TemplateException;
import io.sentry.Sentry;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMailService {

  @NotNull private final SimpleMailService mailService;

  @NotNull private final MailConfigProperties.NewUserMail newUserMailConfig;

  @NotNull private final MailConfigProperties.ResetTokenMail resetTokenMailConfig;

  @NotNull private final MailConfigProperties.Branding mailBranding;
  @NotNull private final MailConfigProperties.NewLoginMail newLoginMailConfig;

  @NotNull private final TranslationService translationService;

  void sendNewUserMail(
      @NotNull final String userMailAddress,
      @NotNull final String resetToken,
      @NotNull final Locale locale)
      throws CheckedMailException {
    final String resetLink = mailBranding.getUrl() + newUserMailConfig.getResetLink();
    final String subject =
        MessageFormat.format(
            translationService
                .translate(newUserMailConfig.getSubjectKey(), locale)
                .orElse("Welcome New User"),
            mailBranding.getName());
    try {
      String message =
          mailService.getMessageFromTemplate(
              newUserMailConfig.getTemplate(),
              locale,
              new ResetTokenMessageData(
                  mailBranding, userMailAddress, resetLink, resetToken, subject));

      var newMail = new Mail(null, Set.of(userMailAddress), subject, message);
      mailService.sendMail(newMail);
    } catch (MailException | TemplateException | IOException e) {
      throw new CheckedMailException(e);
    }
  }

  void sendResetToken(
      @NotNull final String userMailAddress,
      @NotNull final String resetToken,
      @NotNull final Locale locale)
      throws CheckedMailException {
    final String resetLink = mailBranding.getUrl() + resetTokenMailConfig.getResetLink();
    final String subject =
        translationService
            .translate(resetTokenMailConfig.getSubjectKey(), locale)
            .orElse("Reset Password");

    try {
      String message =
          mailService.getMessageFromTemplate(
              resetTokenMailConfig.getTemplate(),
              locale,
              new ResetTokenMessageData(
                  mailBranding, userMailAddress, resetLink, resetToken, subject));

      var newMail = new Mail(null, Set.of(userMailAddress), subject, message);
      mailService.sendMail(newMail);
    } catch (TemplateException | IOException | MailException e) {
      throw new CheckedMailException(e);
    }
  }

  @Async
  public void sendLoginMail(String email, TokenRepresentation tokenRepresentation, Locale locale) {
    final String subject =
        translationService
            .translate(newLoginMailConfig.getSubjectKey(), locale)
            .orElse("New Login");
    try {
      String message =
          mailService.getMessageFromTemplate(
              newLoginMailConfig.getTemplate(),
              locale,
              new LoginMessageData(mailBranding, email, subject, tokenRepresentation));

      var newMail = new Mail(null, Set.of(email), subject, message);
      mailService.sendMail(newMail);
    } catch (MailException | TemplateException | IOException e) {
      Sentry.captureException(e);
    }
  }
}
