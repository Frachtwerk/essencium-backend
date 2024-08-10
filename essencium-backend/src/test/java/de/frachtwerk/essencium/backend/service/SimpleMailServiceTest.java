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

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.configuration.properties.MailConfigProperties;
import de.frachtwerk.essencium.backend.model.Mail;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

class SimpleMailServiceTest {

  private final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
  private final MailConfigProperties.DefaultSender defaultSender =
      Mockito.mock(MailConfigProperties.DefaultSender.class);
  private final MailConfigProperties.DebugReceiver debugReceiver =
      Mockito.mock(MailConfigProperties.DebugReceiver.class);
  private final FreeMarkerConfigurer freemarkerConfigurer =
      Mockito.mock(FreeMarkerConfigurer.class);

  private final SimpleMailService testSubject =
      new SimpleMailService(mailSender, defaultSender, debugReceiver, freemarkerConfigurer);

  @BeforeEach
  void setUp() {
    Mockito.reset(mailSender);
    Mockito.reset(defaultSender);
  }

  @Nested
  class SendMail {
    private final Mail testMail = Mockito.mock(Mail.class);
    private final InputStreamSource attachmentSource = Mockito.mock(InputStreamSource.class);

    private final String defaultSenderAddress = "DEFAULT_SENDER_ADDRESS";

    private final String testSenderAddress = "TEST_SENDER_ADDRESS";
    private final Set<String> testRecipientAddress = Set.of("TEST_RECIPIENT_ADDRESS");
    private final Set<String> debugRecipientAddress = Set.of("DEBUG_RECIPIENT_ADDRESS");
    private final String testMailSubject = "TEST_MAIL_SUBJECT";
    private final String testMailMessage = "TEST_MAIL_MESSAGE";

    @BeforeEach
    void setUp() {
      Mockito.reset(mailSender);

      Mockito.when(defaultSender.getAddress()).thenReturn(defaultSenderAddress);

      Mockito.when(testMail.getSenderAddress()).thenReturn(testSenderAddress);
      Mockito.when(testMail.getRecipientAddress()).thenReturn(testRecipientAddress);
      Mockito.when(testMail.getSubject()).thenReturn(testMailSubject);
      Mockito.when(testMail.getMessage()).thenReturn(testMailMessage);
    }

    @Test
    void noSenderAddress() {
      Mockito.when(testMail.getSenderAddress()).thenReturn(null);

      Mockito.doAnswer(
              invocationOnMock -> {
                SimpleMailMessage passed = invocationOnMock.getArgument(0);

                assertThat(passed.getFrom()).isEqualTo(defaultSenderAddress);
                assertThat(passed.getReplyTo()).isNull();
                assertThat(passed.getTo()).hasSize(1);
                assertThat(Objects.requireNonNull(passed.getTo()))
                    .containsExactlyInAnyOrderElementsOf(testRecipientAddress);
                assertThat(passed.getSubject()).isEqualTo(testMailSubject);
                assertThat(passed.getText()).isEqualTo(testMailMessage);

                return "";
              })
          .when(mailSender)
          .send(Mockito.any(SimpleMailMessage.class));

      testSubject.sendMail(testMail);
    }

    @Test
    void overrideSenderAddress() {
      Mockito.doAnswer(
              invocationOnMock -> {
                SimpleMailMessage passed = invocationOnMock.getArgument(0);

                assertThat(passed.getFrom()).isEqualTo(defaultSenderAddress);
                assertThat(passed.getReplyTo()).isEqualTo(testSenderAddress);
                assertThat(passed.getTo()).hasSize(1);
                assertThat(Objects.requireNonNull(passed.getTo()))
                    .containsExactlyInAnyOrderElementsOf(testRecipientAddress);
                assertThat(passed.getSubject()).isEqualTo(testMailSubject);
                assertThat(passed.getText()).isEqualTo(testMailMessage);

                return "";
              })
          .when(mailSender)
          .send(Mockito.any(SimpleMailMessage.class));

      testSubject.sendMail(testMail);
    }

    @Test
    void overrideReceiverAddress() {
      Mockito.when(debugReceiver.getActive()).thenReturn(true);
      Mockito.when(debugReceiver.getAddress()).thenReturn("DEBUG_RECIPIENT_ADDRESS");
      Mockito.doAnswer(
              invocationOnMock -> {
                SimpleMailMessage passed = invocationOnMock.getArgument(0);

                assertThat(passed.getFrom()).isEqualTo(defaultSenderAddress);
                assertThat(passed.getReplyTo()).isEqualTo(testSenderAddress);
                assertThat(passed.getTo()).hasSize(1);
                assertThat(Objects.requireNonNull(passed.getTo()))
                    .containsExactlyInAnyOrderElementsOf(debugRecipientAddress);
                assertThat(passed.getSubject()).isEqualTo(testMailSubject);
                assertThat(passed.getText()).isEqualTo(testMailMessage);

                return "";
              })
          .when(mailSender)
          .send(Mockito.any(SimpleMailMessage.class));

      testSubject.sendMail(testMail);
    }

    @Test
    void sendMail() {
      Mockito.doAnswer(
              invocationOnMock -> {
                SimpleMailMessage passed = invocationOnMock.getArgument(0);

                assertThat(passed.getFrom()).isEqualTo(testSenderAddress);
                assertThat(passed.getTo()).hasSize(1);
                assertThat(Objects.requireNonNull(passed.getTo()))
                    .containsExactlyInAnyOrderElementsOf(testRecipientAddress);
                assertThat(passed.getSubject()).isEqualTo(testMailSubject);
                assertThat(passed.getText()).isEqualTo(testMailMessage);

                return "";
              })
          .when(mailSender)
          .send(Mockito.any(SimpleMailMessage.class));

      testSubject.sendMail(testMail);
    }

    @Test
    void sendMailWithAttachements() {
      Mockito.doAnswer(
              invocationOnMock -> {
                SimpleMailMessage passed = invocationOnMock.getArgument(0);

                assertThat(passed.getFrom()).isEqualTo(testSenderAddress);
                assertThat(passed.getTo()).hasSize(1);
                assertThat(Objects.requireNonNull(passed.getTo()))
                    .containsExactlyInAnyOrderElementsOf(testRecipientAddress);
                assertThat(passed.getSubject()).isEqualTo(testMailSubject);
                assertThat(passed.getText()).isEqualTo(testMailMessage);

                return "";
              })
          .when(mailSender)
          .send(Mockito.any(SimpleMailMessage.class));

      testSubject.sendMail(testMail, "testPdf.pdf", attachmentSource);
    }
  }
}
