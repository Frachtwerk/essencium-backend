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
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SimpleMailService {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleMailService.class);

  private final JavaMailSender mailSender;
  private final MailConfigProperties.DefaultSender defaultSender;
  private final MailConfigProperties.DebugReceiver debugReceiver;
  private final FreeMarkerConfigurer freemarkerConfigurer;

  @Autowired
  public SimpleMailService(
      @NotNull final JavaMailSender mailSender,
      @NotNull final MailConfigProperties.DefaultSender defaultSender,
      final MailConfigProperties.DebugReceiver debugReceiver,
      @NotNull final FreeMarkerConfigurer freemarkerConfigurer) {
    this.mailSender = mailSender;
    this.freemarkerConfigurer = freemarkerConfigurer;
    this.defaultSender = defaultSender;
    this.debugReceiver = debugReceiver;
  }

  public void sendMail(@NotNull final Mail draftMail) throws MailException {
    MimeMessagePreparator messagePreparator =
        mailMessage -> getDefaultMimeMessageHelper(mailMessage, draftMail);
    mailSender.send(messagePreparator);
  }

  public void sendMail(
      @NotNull final Mail draftMail, String attachmentFileName, InputStreamSource attachmentSource)
      throws MailException {
    MimeMessagePreparator messagePreparator =
        mailMessage -> {
          MimeMessageHelper helper = getDefaultMimeMessageHelper(mailMessage, draftMail);
          helper.addAttachment(attachmentFileName, attachmentSource);
        };
    mailSender.send(messagePreparator);
  }

  private MimeMessageHelper getDefaultMimeMessageHelper(
      MimeMessage mailMessage, @NotNull Mail draftMail)
      throws MessagingException, UnsupportedEncodingException {
    MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
    helper.setFrom(defaultSender.getAddress(), defaultSender.getName());
    if (draftMail.getSenderAddress() != null) {
      helper.setReplyTo(draftMail.getSenderAddress());
    }
    mailMessage.setSubject(draftMail.getSubject());
    helper.setText(draftMail.getMessage(), true);

    Optional.ofNullable(debugReceiver)
        .filter(MailConfigProperties.DebugReceiver::getActive)
        .ifPresent(
            debugReceiver -> {
              LOG.debug(
                  "Overwriting recipient address with debug receiver {}.",
                  debugReceiver.getAddress());
              draftMail.setRecipientAddress(Set.of(debugReceiver.getAddress()));
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
