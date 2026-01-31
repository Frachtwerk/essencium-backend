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

import de.frachtwerk.essencium.backend.configuration.properties.MailProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.sentry.Sentry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Service
@Slf4j
public class SimpleMailService {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final FreeMarkerConfigurer freemarkerConfigurer;

  @Autowired
  public SimpleMailService(
      @NotNull final JavaMailSender mailSender,
      @NotNull final MailProperties mailProperties,
      @NotNull final FreeMarkerConfigurer freemarkerConfigurer) {
    this.mailSender = mailSender;
    this.freemarkerConfigurer = freemarkerConfigurer;
    this.mailProperties = mailProperties;
  }

  public void sendMail(@NotNull final Mail draftMail) {
    if (mailProperties.isEnabled()) {
      MimeMessagePreparator messagePreparator =
          mailMessage -> getDefaultMimeMessageHelper(mailMessage, draftMail);
      try {
        mailSender.send(messagePreparator);
      } catch (MailException e) {
        Sentry.captureException(e);
        log.error("Error while sending mail", e);
      }
    } else {
      logDisabledMailService(draftMail);
    }
  }

  private static void logDisabledMailService(Mail draftMail) {
    log.info(
        "Mail service is disabled. Not sending mail to {} with subject '{}'.",
        draftMail.getRecipientAddress(),
        draftMail.getSubject());
  }

  public void sendMail(
      @NotNull final Mail draftMail,
      String attachmentFileName,
      InputStreamSource attachmentSource) {
    if (mailProperties.isEnabled()) {
      MimeMessagePreparator messagePreparator =
          mailMessage -> {
            MimeMessageHelper helper = getDefaultMimeMessageHelper(mailMessage, draftMail);
            helper.addAttachment(attachmentFileName, attachmentSource);
          };
      try {
        mailSender.send(messagePreparator);
      } catch (MailException e) {
        Sentry.captureException(e);
        log.error("Error while sending mail", e);
      }
    } else {
      logDisabledMailService(draftMail);
    }
  }

  private MimeMessageHelper getDefaultMimeMessageHelper(
      MimeMessage mailMessage, @NotNull Mail draftMail)
      throws MessagingException, UnsupportedEncodingException {
    MailProperties.DefaultSender defaultSender = mailProperties.getDefaultSender();
    MailProperties.DebugReceiver debugReceiver = mailProperties.getDebugReceiver();
    MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
    helper.setFrom(defaultSender.getAddress(), defaultSender.getName());
    if (draftMail.getSenderAddress() != null) {
      helper.setReplyTo(draftMail.getSenderAddress());
    }
    mailMessage.setSubject(draftMail.getSubject());
    helper.setText(draftMail.getMessage(), true);

    Optional.ofNullable(debugReceiver)
        .filter(MailProperties.DebugReceiver::getActive)
        .ifPresent(
            receiver -> {
              log.debug(
                  "Overwriting recipient address with debug receiver {}.", receiver.getAddress());
              draftMail.setRecipientAddress(Set.of(receiver.getAddress()));
            });
    helper.setTo(draftMail.getRecipientAddress().toArray(String[]::new));
    return helper;
  }

  public String getMessageFromTemplate(
      @NotNull final String template,
      @NotNull final Locale locale,
      @NotNull final Object dataObject)
      throws IOException, TemplateException {
    Template freemarkerTemplate =
        freemarkerConfigurer.getConfiguration().getTemplate(template, locale);

    return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, dataObject);
  }
}
