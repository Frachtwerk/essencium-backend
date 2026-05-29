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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.configuration.properties.MailProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.sentry.Sentry;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

class SimpleMailServiceTest {

  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final MailProperties.DefaultSender defaultSender =
      mock(MailProperties.DefaultSender.class);
  private final MailProperties.DebugReceiver debugReceiver =
      mock(MailProperties.DebugReceiver.class);
  private final FreeMarkerConfigurer freemarkerConfigurer = mock(FreeMarkerConfigurer.class);

  private final MailProperties mailProperties = mock(MailProperties.class);

  private final SimpleMailService testSubject =
      new SimpleMailService(mailSender, mailProperties, freemarkerConfigurer);

  private static final String DEFAULT_SENDER_ADDRESS = "noreply@example.org";
  private static final String DEFAULT_SENDER_NAME = "Essencium";

  @BeforeEach
  void setUp() {
    when(mailProperties.getDefaultSender()).thenReturn(defaultSender);
    when(mailProperties.isEnabled()).thenReturn(true);
    when(mailProperties.getDebugReceiver()).thenReturn(debugReceiver);

    when(mailProperties.getDebugReceiver()).thenReturn(debugReceiver);
    when(debugReceiver.getActive()).thenReturn(false);

    when(defaultSender.getAddress()).thenReturn(DEFAULT_SENDER_ADDRESS);
    when(defaultSender.getName()).thenReturn(DEFAULT_SENDER_NAME);
  }

  @AfterEach
  void tearDown() {
    // Keep tests isolated because send() interactions are asserted per test.
    org.mockito.Mockito.reset(mailSender);
  }

  @Nested
  class SendMail {
    private final String senderAddress = "author@example.org";
    private final Set<String> recipientAddress = Set.of("team@example.org");
    private final String subject = "Subject";
    private final String htmlMessage = "<p>Hello</p>";

    private Mail createMail(String sender, Set<String> recipients) {
      return new Mail(sender, recipients, subject, htmlMessage);
    }

    @Test
    void sendMail_setsFromReplyToRecipientAndContent() throws Exception {
      Mail draftMail = createMail(senderAddress, recipientAddress);

      testSubject.sendMail(draftMail);

      MimeMessage mimeMessage = capturePreparedMimeMessage();
      assertSender(mimeMessage, DEFAULT_SENDER_ADDRESS, DEFAULT_SENDER_NAME);
      assertReplyTo(mimeMessage, senderAddress);
      assertRecipients(mimeMessage, recipientAddress);
      assertThat(mimeMessage.getSubject()).isEqualTo(subject);
      assertThat(asRawMail(mimeMessage)).contains("Hello");
    }

    @Test
    void sendMail_withoutSender_doesNotSetReplyTo() throws Exception {
      Mail draftMail = createMail(null, recipientAddress);

      testSubject.sendMail(draftMail);

      MimeMessage mimeMessage = capturePreparedMimeMessage();
      assertSender(mimeMessage, DEFAULT_SENDER_ADDRESS, DEFAULT_SENDER_NAME);
      assertReplyTo(mimeMessage, DEFAULT_SENDER_ADDRESS);
      assertRecipients(mimeMessage, recipientAddress);
    }

    @Test
    void sendMail_withActiveDebugReceiver_overwritesRecipients() throws Exception {
      when(debugReceiver.getActive()).thenReturn(true);
      when(debugReceiver.getAddress()).thenReturn("debug@example.org");
      Mail draftMail = createMail(senderAddress, recipientAddress);

      testSubject.sendMail(draftMail);

      MimeMessage mimeMessage = capturePreparedMimeMessage();
      assertRecipients(mimeMessage, Set.of("debug@example.org"));
      assertThat(draftMail.getRecipientAddress()).containsExactly("debug@example.org");
    }

    @Test
    void sendMail_withNullDebugReceiver_keepsRecipients() throws Exception {
      when(mailProperties.getDebugReceiver()).thenReturn(null);
      Mail draftMail = createMail(senderAddress, recipientAddress);

      testSubject.sendMail(draftMail);

      MimeMessage mimeMessage = capturePreparedMimeMessage();
      assertRecipients(mimeMessage, recipientAddress);
    }

    @Test
    void sendMail_whenDisabled_doesNotSend() {
      when(mailProperties.isEnabled()).thenReturn(false);
      Mail draftMail = createMail(senderAddress, recipientAddress);

      testSubject.sendMail(draftMail);

      verify(mailSender, never()).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendMail_whenSenderThrows_capturesExceptionInSentry() {
      Mail draftMail = createMail(senderAddress, recipientAddress);
      MailSendException expectedException = new MailSendException("mail transport failed");
      org.mockito.Mockito.doThrow(expectedException)
          .when(mailSender)
          .send(any(MimeMessagePreparator.class));

      try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
        testSubject.sendMail(draftMail);

        sentry.verify(() -> Sentry.captureException(expectedException));
      }
    }

    @Test
    void sendMailWithAttachment_addsAttachmentToMessage() throws Exception {
      Mail draftMail = createMail(senderAddress, recipientAddress);
      InputStreamSource source =
          new ByteArrayResource("pdf-content".getBytes(StandardCharsets.UTF_8));

      testSubject.sendMail(draftMail, "testPdf.pdf", source);

      MimeMessage mimeMessage = capturePreparedMimeMessage();
      assertRecipients(mimeMessage, recipientAddress);
      MimeMultipart multipart = (MimeMultipart) mimeMessage.getContent();
      assertThat(multipart.getCount()).isGreaterThanOrEqualTo(2);

      BodyPart attachmentPart = multipart.getBodyPart(1);
      assertThat(attachmentPart.getFileName()).isEqualTo("testPdf.pdf");
    }

    @Test
    void sendMailWithAttachment_whenDisabled_doesNotSend() {
      when(mailProperties.isEnabled()).thenReturn(false);
      Mail draftMail = createMail(senderAddress, recipientAddress);

      testSubject.sendMail(draftMail, "testPdf.pdf", new ByteArrayResource(new byte[] {1}));

      verify(mailSender, never()).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendMailWithAttachment_whenSenderThrows_capturesExceptionInSentry() {
      Mail draftMail = createMail(senderAddress, recipientAddress);
      MailSendException expectedException = new MailSendException("attachment mail failed");
      org.mockito.Mockito.doThrow(expectedException)
          .when(mailSender)
          .send(any(MimeMessagePreparator.class));

      try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
        testSubject.sendMail(draftMail, "testPdf.pdf", new ByteArrayResource(new byte[] {1}));

        sentry.verify(() -> Sentry.captureException(expectedException));
      }
    }

    private MimeMessage capturePreparedMimeMessage() throws Exception {
      ArgumentCaptor<MimeMessagePreparator> preparatorCaptor =
          ArgumentCaptor.forClass(MimeMessagePreparator.class);
      verify(mailSender).send(preparatorCaptor.capture());

      MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
      preparatorCaptor.getValue().prepare(mimeMessage);
      return mimeMessage;
    }

    private void assertSender(MimeMessage message, String address, String personalName)
        throws Exception {
      InternetAddress from = (InternetAddress) message.getFrom()[0];
      assertThat(from.getAddress()).isEqualTo(address);
      assertThat(from.getPersonal()).isEqualTo(personalName);
    }

    private void assertReplyTo(MimeMessage message, String address) throws Exception {
      InternetAddress replyTo = (InternetAddress) message.getReplyTo()[0];
      assertThat(replyTo.getAddress()).isEqualTo(address);
    }

    private void assertRecipients(MimeMessage message, Set<String> expectedRecipients)
        throws Exception {
      Address[] recipients = message.getAllRecipients();
      assertThat(recipients).hasSize(expectedRecipients.size());
      assertThat(recipients)
          .extracting(address -> ((InternetAddress) address).getAddress())
          .containsExactlyInAnyOrderElementsOf(expectedRecipients);
    }

    private String asRawMail(MimeMessage mimeMessage) throws Exception {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mimeMessage.writeTo(outputStream);
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  @Nested
  class TemplateMessages {

    @Test
    void getMessageFromTemplate_processesTemplateWithData() throws Exception {
      Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
      StringTemplateLoader templateLoader = new StringTemplateLoader();
      templateLoader.putTemplate("welcome.ftl", "Hallo ${name}!");
      configuration.setTemplateLoader(templateLoader);
      configuration.setDefaultEncoding("UTF-8");
      when(freemarkerConfigurer.getConfiguration()).thenReturn(configuration);

      String message =
          testSubject.getMessageFromTemplate("welcome.ftl", Locale.GERMAN, Map.of("name", "Paul"));

      assertThat(message).isEqualTo("Hallo Paul!");
    }

    @Test
    void getMessageFromTemplate_throwsIOExceptionIfTemplateMissing() {
      Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
      configuration.setTemplateLoader(new StringTemplateLoader());
      when(freemarkerConfigurer.getConfiguration()).thenReturn(configuration);

      assertThatThrownBy(
              () -> testSubject.getMessageFromTemplate("missing.ftl", Locale.GERMAN, Map.of()))
          .isInstanceOf(IOException.class);
    }

    @Test
    void getMessageFromTemplate_throwsTemplateExceptionForMissingVariable() {
      Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
      StringTemplateLoader templateLoader = new StringTemplateLoader();
      templateLoader.putTemplate("strict.ftl", "Hallo ${name?upper_case}!");
      configuration.setTemplateLoader(templateLoader);
      configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      configuration.setLogTemplateExceptions(false);
      when(freemarkerConfigurer.getConfiguration()).thenReturn(configuration);

      assertThatThrownBy(
              () -> testSubject.getMessageFromTemplate("strict.ftl", Locale.GERMAN, Map.of()))
          .isInstanceOf(TemplateException.class);
    }
  }
}
