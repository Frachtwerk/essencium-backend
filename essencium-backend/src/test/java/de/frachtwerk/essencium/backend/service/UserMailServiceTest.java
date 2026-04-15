/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.configuration.properties.MailProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import de.frachtwerk.essencium.backend.model.mail.LoginMessageData;
import de.frachtwerk.essencium.backend.model.mail.ResetTokenMessageData;
import de.frachtwerk.essencium.backend.model.representation.TokenRepresentation;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;

@ExtendWith(MockitoExtension.class)
class UserMailServiceTest {

  @Mock SimpleMailService mailServiceMock;
  @Mock TranslationService translationServiceMock;

  @Mock MailProperties.NewUserMail newUserEmailProperties;
  @Mock MailProperties.ResetTokenMail resetTokenMailProperties;
  @Mock MailProperties.Branding brandingProperties;
  @Mock MailProperties.NewLoginMail newLoginMailProperties;

  @InjectMocks UserMailService testSubject;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(
        mailServiceMock,
        translationServiceMock,
        newUserEmailProperties,
        resetTokenMailProperties,
        brandingProperties,
        newLoginMailProperties);
  }

  @Test
  void sendNewUserMail() throws MailException, IOException, TemplateException {
    var userMail = "USER_MAIL_USERNAME";
    var resetToken = "RESET_TOKEN";
    var locale = Locale.GERMANY;
    var testTemplate = "NewUserMessage.ftl";

    var subjectKey = "mail.new-user.subject";
    var subject = "SUBJECT";

    when(newUserEmailProperties.getSubjectKey()).thenReturn(subjectKey);
    when(newUserEmailProperties.getTemplate()).thenReturn(testTemplate);
    when(newUserEmailProperties.isEnabled()).thenReturn(true);
    when(translationServiceMock.translate(anyString(), any(Locale.class)))
        .thenReturn(Optional.of(subject));

    doAnswer(
            invocationOnMock -> {
              final Object dataObject = invocationOnMock.getArgument(2);
              return dataObject.toString();
            })
        .when(mailServiceMock)
        .getMessageFromTemplate(
            any(String.class), any(Locale.class), any(ResetTokenMessageData.class));

    Mockito.doAnswer(
            invocationOnMock -> {
              var mailToSend = invocationOnMock.getArgument(0, Mail.class);
              assertThat(mailToSend.getSenderAddress()).isNull();
              assertThat(mailToSend.getRecipientAddress()).contains(userMail);
              assertThat(mailToSend.getSubject()).isEqualTo(subject);
              assertThat(mailToSend.getMessage()).contains(resetToken);
              return "";
            })
        .when(mailServiceMock)
        .sendMail(any(Mail.class));

    testSubject.sendNewUserMail(userMail, resetToken, locale);
    verify(brandingProperties).getUrl();
    verify(brandingProperties).getName();
    verify(newUserEmailProperties).getResetLink();
  }

  @Test
  void sendNewUserMail_disabled() {
    when(newUserEmailProperties.isEnabled()).thenReturn(false);

    testSubject.sendNewUserMail("userMail", "resetToken", Locale.getDefault());

    verify(newUserEmailProperties).isEnabled();
    verifyNoMoreInteractions(newUserEmailProperties);
    verify(mailServiceMock, never()).sendMail(any(Mail.class));
    verifyNoInteractions(
        mailServiceMock,
        resetTokenMailProperties,
        brandingProperties,
        newLoginMailProperties,
        translationServiceMock);
  }

  @Test
  void sendResetToken() throws MailException, IOException, TemplateException {
    var testMail = "test@example.com";
    var testToken = "BANANARAMA";
    var locale = Locale.GERMANY;
    var testTemplate = "ResetTokenMessage.ftl";

    var subjectKey = "mail.new-user.subject";
    var subject = "SUBJECT";

    when(resetTokenMailProperties.getSubjectKey()).thenReturn(subjectKey);
    when(resetTokenMailProperties.getTemplate()).thenReturn(testTemplate);
    when(resetTokenMailProperties.isEnabled()).thenReturn(true);
    when(translationServiceMock.translate(anyString(), any(Locale.class)))
        .thenReturn(Optional.of(subject));

    doAnswer(
            invocationOnMock -> {
              final Object dataObject = invocationOnMock.getArgument(2);
              return dataObject.toString();
            })
        .when(mailServiceMock)
        .getMessageFromTemplate(anyString(), any(Locale.class), any(ResetTokenMessageData.class));

    Mockito.doAnswer(
            invocationOnMock -> {
              var mailToSend = invocationOnMock.getArgument(0, Mail.class);

              assertThat(mailToSend.getSenderAddress()).isNull();
              assertThat(mailToSend.getRecipientAddress()).containsExactlyInAnyOrder(testMail);
              assertThat(mailToSend.getSubject()).isEqualTo(subject);
              assertThat(mailToSend.getMessage()).contains(testToken);

              return "";
            })
        .when(mailServiceMock)
        .sendMail(any(Mail.class));

    testSubject.sendResetToken(testMail, testToken, locale);
    verify(brandingProperties).getUrl();
    verify(resetTokenMailProperties).getResetLink();
  }

  @Test
  void sendResetToken_disabled() {
    when(resetTokenMailProperties.isEnabled()).thenReturn(false);
    testSubject.sendResetToken("userMail", "resetToken", Locale.getDefault());
    verify(resetTokenMailProperties).isEnabled();
    verifyNoMoreInteractions(resetTokenMailProperties);
    verify(mailServiceMock, never()).sendMail(any(Mail.class));
    verifyNoMoreInteractions(
        mailServiceMock,
        resetTokenMailProperties,
        brandingProperties,
        newLoginMailProperties,
        translationServiceMock);
  }

  @Test
  void sendNewLoginMail() throws MailException, IOException, TemplateException {
    String testMail = "test@example.com";
    Locale locale = Locale.GERMANY;
    String testTemplate = "NewLoginMessage.ftl";
    String subjectKey = "mail.new-login.subject";
    String subject = "SUBJECT";
    Date date = new Date();
    TokenRepresentation tokenRepresentation =
        TokenRepresentation.builder()
            .id(UUID.randomUUID())
            .issuedAt(date)
            .expiration(date)
            .lastUsed(LocalDateTime.now())
            .userAgent("fakeUserAgent")
            .build();

    when(newLoginMailProperties.getSubjectKey()).thenReturn(subjectKey);
    when(newLoginMailProperties.getTemplate()).thenReturn(testTemplate);
    when(newLoginMailProperties.isEnabled()).thenReturn(true);
    when(translationServiceMock.translate(anyString(), any(Locale.class)))
        .thenReturn(Optional.of(subject));
    when(mailServiceMock.getMessageFromTemplate(
            anyString(), any(Locale.class), any(LoginMessageData.class)))
        .thenAnswer(
            invocationOnMock -> {
              final Object dataObject = invocationOnMock.getArgument(2);
              return dataObject.toString();
            });

    Mockito.doAnswer(
            invocationOnMock -> {
              Mail mailToSend = invocationOnMock.getArgument(0, Mail.class);
              assertThat(mailToSend.getSenderAddress()).isNull();
              assertThat(mailToSend.getRecipientAddress()).containsExactlyInAnyOrder(testMail);
              assertThat(mailToSend.getSubject()).isEqualTo(subject);
              assertThat(mailToSend.getMessage())
                  .contains(tokenRepresentation.getIssuedAt().toString());
              assertThat(mailToSend.getMessage()).contains(tokenRepresentation.getUserAgent());
              return "";
            })
        .when(mailServiceMock)
        .sendMail(any(Mail.class));

    assertThatNoException()
        .isThrownBy(() -> testSubject.sendLoginMail(testMail, tokenRepresentation, locale));

    verify(translationServiceMock, times(1)).translate(anyString(), any(Locale.class));
    verify(mailServiceMock, times(1))
        .getMessageFromTemplate(anyString(), any(Locale.class), any(LoginMessageData.class));
    verify(mailServiceMock, times(1)).sendMail(any(Mail.class));
    verifyNoMoreInteractions(translationServiceMock);
    verifyNoMoreInteractions(mailServiceMock);
  }

  @Test
  void sendNewLoginMail_disabled() {
    when(newLoginMailProperties.isEnabled()).thenReturn(false);
    testSubject.sendLoginMail(
        "userMail", TokenRepresentation.builder().build(), Locale.getDefault());

    verify(newLoginMailProperties).isEnabled();
    verifyNoMoreInteractions(newLoginMailProperties);
    verify(mailServiceMock, never()).sendMail(any(Mail.class));
    verifyNoMoreInteractions(
        mailServiceMock,
        resetTokenMailProperties,
        brandingProperties,
        newLoginMailProperties,
        translationServiceMock);
  }
}
