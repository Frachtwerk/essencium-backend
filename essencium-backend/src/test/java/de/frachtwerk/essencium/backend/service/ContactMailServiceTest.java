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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.configuration.properties.MailProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import de.frachtwerk.essencium.backend.model.dto.BaseUserDto;
import de.frachtwerk.essencium.backend.model.dto.ContactRequestDto;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.exception.InvalidInputException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.mail.ContactMessageData;
import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@ExtendWith(MockitoExtension.class)
class ContactMailServiceTest {

  @Mock SimpleMailService mailServiceMock;

  @Mock
  AbstractUserService<UserStub, EssenciumUserDetails<Long>, Long, BaseUserDto<Long>>
      userServiceMock;

  @Mock MailProperties.ContactMail contactMailConfigPropertiesMock;
  @Mock MailProperties.Branding brandingConfigPropertiesMock;
  @Mock TranslationService translationServiceMock;

  private ContactMailService testSubject;

  @BeforeEach
  void setUp() {
    reset(mailServiceMock, userServiceMock, contactMailConfigPropertiesMock);
    testSubject =
        new ContactMailService(
            mailServiceMock,
            contactMailConfigPropertiesMock,
            brandingConfigPropertiesMock,
            translationServiceMock);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(
        contactMailConfigPropertiesMock,
        brandingConfigPropertiesMock,
        mailServiceMock,
        userServiceMock,
        translationServiceMock);
  }

  @Nested
  class SendContactRequest {

    private static final String testUserName = "TEST_USER_NAME";
    private static final String testUserEMail = "TEST_MAIL_ADDRESS";
    private static final String testUserFirstName = "TEST_USER_FIRST_NAME";
    private static final String testUserLastName = "TEST_USER_LAST_NAME";

    private final EssenciumUserDetails<?> testUser = mock(EssenciumUserDetails.class);

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
      lenient()
          .doAnswer(
              invocationOnMock -> {
                final Principal principal = invocationOnMock.getArgument(0);

                if (principal == null || principal.getName() == null) {
                  throw new SessionAuthenticationException("Unauthorized");
                } else if (!principal.getName().equals(testUserName)) {
                  throw new ResourceNotFoundException();
                } else {
                  return testUser;
                }
              })
          .when(userServiceMock)
          .getAUTHUSERFromPrincipal(any());

      when(testUser.getUsername()).thenReturn(testUserEMail);
      when(testUser.getFirstName()).thenReturn(testUserFirstName);
      when(testUser.getLastName()).thenReturn(testUserLastName);

      testRequest.setSubject(testRequestSubject);
      testRequest.setMessage(testRequestMessage);

      lenient().when(contactMailConfigPropertiesMock.getRecipients()).thenReturn(testRecipients);
      lenient()
          .when(contactMailConfigPropertiesMock.getSubjectPrefixKey())
          .thenReturn(testPrefixKey);
      lenient().when(contactMailConfigPropertiesMock.getLocale()).thenReturn(testLocale);
      lenient().when(contactMailConfigPropertiesMock.getTemplate()).thenReturn(testTemplate);
      lenient().when(contactMailConfigPropertiesMock.isEnabled()).thenReturn(true);
      lenient()
          .when(translationServiceMock.translate(any(String.class), any(Locale.class)))
          .thenReturn(Optional.of(testPrefix));

      lenient()
          .doAnswer(
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
      verify(contactMailConfigPropertiesMock).isEnabled();
    }

    @SneakyThrows
    @Test
    void issuingInformationFromUser() {
      var AUTHUSER = mock(EssenciumUserDetails.class);
      when(AUTHUSER.getUsername()).thenReturn(testUserEMail);
      when(AUTHUSER.getFirstName()).thenReturn(testUserFirstName);
      when(AUTHUSER.getLastName()).thenReturn(testUserLastName);
      doAnswer(
              invocationOnMock -> {
                final Mail mailToSend = invocationOnMock.getArgument(0);

                evaluateMail(mailToSend, testUserFirstName + " " + testUserLastName, testUserEMail);

                return "";
              })
          .when(mailServiceMock)
          .sendMail(any(Mail.class));

      when(contactMailConfigPropertiesMock.isEnabled()).thenReturn(true);

      testSubject.sendContactRequest(testRequest, AUTHUSER);

      verify(contactMailConfigPropertiesMock).isEnabled();
      verify(contactMailConfigPropertiesMock).getTemplate();
      verify(contactMailConfigPropertiesMock, times(2)).getLocale();
      verify(contactMailConfigPropertiesMock).getSubjectPrefixKey();
      verify(contactMailConfigPropertiesMock).getRecipients();
      verify(mailServiceMock).getMessageFromTemplate(anyString(), any(Locale.class), any());
      verify(translationServiceMock).translate(anyString(), any(Locale.class));
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

      when(contactMailConfigPropertiesMock.isEnabled()).thenReturn(true);

      testSubject.sendContactRequest(testRequest, testUser);

      verify(contactMailConfigPropertiesMock).isEnabled();
      verify(contactMailConfigPropertiesMock).getTemplate();
      verify(contactMailConfigPropertiesMock, times(2)).getLocale();
      verify(contactMailConfigPropertiesMock).getSubjectPrefixKey();
      verify(contactMailConfigPropertiesMock).getRecipients();
      verify(mailServiceMock).getMessageFromTemplate(anyString(), any(Locale.class), any());
      verify(translationServiceMock).translate(anyString(), any(Locale.class));
    }

    @Test
    void disabled() {
      when(contactMailConfigPropertiesMock.isEnabled()).thenReturn(false);
      testSubject.sendContactRequest(testRequest, testUser);
      verify(contactMailConfigPropertiesMock).isEnabled();
      verifyNoInteractions(mailServiceMock);
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
