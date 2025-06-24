/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import de.frachtwerk.essencium.backend.model.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.Mail;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.mail.ContactMessageData;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import freemarker.template.TemplateException;
import io.sentry.Sentry;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMailService<USER extends EssenciumUserDetails<ID>, ID extends Serializable> {

  @NotNull private final SimpleMailService mailService;

  @NotNull private final MailConfigProperties.ContactMail contactMailConfig;

  @NotNull private final MailConfigProperties.Branding mailBranding;

  @NotNull private final TranslationService translationService;

  public void sendContactRequest(
      @NotNull final ContactRequestDto contactRequest, final USER issuingUser) {
    try {
      final Mail mailToSend =
          buildMailFromRequest(sanitizeUserInformation(contactRequest, issuingUser));
      mailService.sendMail(mailToSend);
    } catch (IOException | TemplateException e) {
      Sentry.captureException(e);
      log.error("Error while sending contact request mail", e);
    }
  }

  @NotNull
  private Mail buildMailFromRequest(@NotNull final ContactRequestDto contactRequest)
      throws IOException, TemplateException {
    ContactMessageData messageData = new ContactMessageData(mailBranding, contactRequest);

    final String message =
        mailService.getMessageFromTemplate(
            contactMailConfig.getTemplate(), contactMailConfig.getLocale(), messageData);

    final String subject =
        translationService
            .translate(contactMailConfig.getSubjectPrefixKey(), contactMailConfig.getLocale())
            .map(t -> String.format("%s: %s", t, contactRequest.getSubject()))
            .orElse(contactRequest.getSubject());

    return new Mail(
        contactRequest.getMailAddress(), contactMailConfig.getRecipients(), subject, message);
  }

  private @NotNull ContactRequestDto sanitizeUserInformation(
      final @NotNull ContactRequestDto contactRequest, final @Nullable USER issuingUser) {
    if (StringUtils.isBlank(contactRequest.getName())
        || StringUtils.isBlank(contactRequest.getMailAddress())) {
      if (issuingUser == null) {
        throw new InvalidInputException("No name for contact request provided");
      }
      contactRequest.setName(issuingUser.getFirstName() + " " + issuingUser.getLastName());
      contactRequest.setMailAddress(issuingUser.getUsername());
    }

    if (StringUtils.isBlank(contactRequest.getMailAddress())) {
      throw new InvalidInputException("No mail for contact request provided");
    }
    return contactRequest;
  }
}
