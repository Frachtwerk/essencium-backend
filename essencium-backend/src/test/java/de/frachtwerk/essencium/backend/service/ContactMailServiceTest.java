/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.configuration.properties.MailConfigProperties;
import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Mail;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.UnauthorizedException;
import de.frachtwerk.essencium.backend.model.mail.ContactMessageData;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactMailServiceTest {

  private final SimpleMailService mailServiceMock = mock(SimpleMailService.class);
  private final AbstractUserService<TestLongUser, Long, UserDto<Long>> userServiceMock =
      mock(AbstractUserService.class);
  private final MailConfigProperties.ContactMail contactMailConfigPropertiesMock =
      mock(MailConfigProperties.ContactMail.class);
  private final MailConfigProperties.Branding brandingConfigPropertiesMock =
      mock(MailConfigProperties.Branding.class);
  private final TranslationService translationServiceMock = mock(TranslationService.class);

  private final ContactMailService testSubject =
      new ContactMailService(
          mailServiceMock,
          contactMailConfigPropertiesMock,
          brandingConfigPropertiesMock,
          translationServiceMock);

  @BeforeEach
  void setUp() {
    reset(mailServiceMock);
    reset(userServiceMock);
    reset(contactMailConfigPropertiesMock);
  }

  @Nested
  class SendContactRequest {

    private static final String testUserName = "TEST_USER_NAME";
    private static final String testUserEMail = "TEST_MAIL_ADDRESS";
    private static final String testUserFirstName = "TEST_USER_FIRST_NAME";
    private static final String testUserLastName = "TEST_USER_LAST_NAME";

    private final AbstractBaseUser testUser = mock(AbstractBaseUser.class);

    private static final String testRequestSubject = "TEST_MESSAGE_SUBJECT";
    private static final String testRequestMessage = "TEST_MESSAGE_REQUEST";

    private final ContactRequestDto testRequest =
        new ContactRequestDto(null, null, testRequestSubject, testRequestMessage);

    private final Set<String> testRecipients =
        Set.of("RECIPIENT_0", "RECIPIENT_2", "RECIPIENT_3", "RECIPIENT_4", "RECIPIENT_5");

    private static final String testPrefix = "Prefix";
    private static final String testPrefixKey = "mail.contact.subject.prefix";
    private static final String testTemplate = "ContactMessage.ftl";
    private final Locale testLocale = Locale.GERMANY;

    @BeforeEach
    void setUp() throws IOException, TemplateException {
      doAnswer(
              invocationOnMock -> {
                final Principal principal = invocationOnMock.getArgument(0);

                if (principal == null || principal.getName() == null) {
                  throw new UnauthorizedException("Unauthorized!");
                } else if (!principal.getName().equals(testUserName)) {
                  throw new ResourceNotFoundException();
                } else {
                  return testUser;
                }
              })
          .when(userServiceMock)
          .getUserFromPrincipal(any());

      when(testUser.getEmail()).thenReturn(testUserEMail);
      when(testUser.getFirstName()).thenReturn(testUserFirstName);
      when(testUser.getLastName()).thenReturn(testUserLastName);

      testRequest.setSubject(testRequestSubject);
      testRequest.setMessage(testRequestMessage);

      when(contactMailConfigPropertiesMock.getRecipients()).thenReturn(testRecipients);
      when(contactMailConfigPropertiesMock.getSubjectPrefixKey()).thenReturn(testPrefixKey);
      when(contactMailConfigPropertiesMock.getLocale()).thenReturn(testLocale);
      when(contactMailConfigPropertiesMock.getTemplate()).thenReturn(testTemplate);
      when(translationServiceMock.translate(any(String.class), any(Locale.class)))
          .thenReturn(Optional.of(testPrefix));

      doAnswer(
              invocationOnMock -> {
                final Object dataObject = invocationOnMock.getArgument(2);
                return dataObject.toString();
              })
          .when(mailServiceMock)
          .getMessageFromTemplate(
              any(String.class), any(Locale.class), any(ContactMessageData.class));
    }

    @Test
    void noIssuingInformation_nothing() {
      assertThatThrownBy(() -> testSubject.sendContactRequest(testRequest, null))
          .isInstanceOf(InvalidInputException.class);
    }

    @SneakyThrows
    @Test
    void issuingInformationFromUser() {
      doAnswer(
              invocationOnMock -> {
                final Mail mailToSend = invocationOnMock.getArgument(0);

                evaluateMail(mailToSend, testUserFirstName + " " + testUserLastName, testUserEMail);

                return "";
              })
          .when(mailServiceMock)
          .sendMail(any(Mail.class));

      testSubject.sendContactRequest(testRequest, testUser);
    }

    @SneakyThrows
    @Test
    void issuingInformationFromRequest() {
      final var requestName = "REQUEST_NAME";
      final var requestEMail = "REQUEST_EMAIL";

      testRequest.setName(requestName);
      testRequest.setMailAddress(requestEMail);

      doAnswer(
              invocationOnMock -> {
                final Mail mailToSend = invocationOnMock.getArgument(0);

                evaluateMail(mailToSend, requestName, requestEMail);

                return "";
              })
          .when(mailServiceMock)
          .sendMail(any(Mail.class));

      testSubject.sendContactRequest(testRequest, testUser);
    }

    private void evaluateMail(
        final Mail mailToSend, final String expectedContactName, final String expectedContactMail) {
      assertThat(mailToSend.getSenderAddress()).isEqualTo(expectedContactMail);
      assertThat(mailToSend.getRecipientAddress())
          .containsExactlyInAnyOrderElementsOf(testRecipients);
      assertThat(mailToSend.getSubject())
          .isEqualTo(String.format("%s: %s", testPrefix, testRequestSubject));

      var message = mailToSend.getMessage();
      assertThat(message).contains(expectedContactName);
      assertThat(message).contains(expectedContactMail);
      assertThat(message).contains(testRequestMessage);
    }
  }
}
