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

package de.frachtwerk.essencium.backend.configuration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.essencium.backend.configuration.properties.MailProperties.Branding;
import de.frachtwerk.essencium.backend.configuration.properties.MailProperties.SMTP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailPropertiesTest {

  @Nested
  class GetMailSender {

    private MailProperties mailProperties(boolean auth, boolean startTls) {
      MailProperties properties = new MailProperties();
      properties.setHost("smtp.example.com");
      properties.setPort(587);
      properties.setUsername("mailer");
      properties.setPassword("s3cret");

      SMTP smtp = new SMTP();
      smtp.setAuth(auth);
      smtp.setStartTls(startTls);
      properties.setSmtp(smtp);
      return properties;
    }

    @Test
    void configuresTransportFromProperties() {
      JavaMailSender sender = mailProperties(true, true).getMailSender();

      assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
      JavaMailSenderImpl impl = (JavaMailSenderImpl) sender;
      assertThat(impl.getHost()).isEqualTo("smtp.example.com");
      assertThat(impl.getPort()).isEqualTo(587);
      assertThat(impl.getUsername()).isEqualTo("mailer");
      assertThat(impl.getPassword()).isEqualTo("s3cret");
      assertThat(impl.getProtocol()).isEqualTo("smtp");
    }

    @Test
    void propagatesSmtpAuthAndStartTlsFlags() {
      JavaMailSenderImpl impl = (JavaMailSenderImpl) mailProperties(true, false).getMailSender();

      assertThat(impl.getJavaMailProperties())
          .containsEntry("mail.smtp.auth", true)
          .containsEntry("mail.smtp.starttls.enable", false);
    }

    @Test
    void propagatesDisabledSmtpFlags() {
      JavaMailSenderImpl impl = (JavaMailSenderImpl) mailProperties(false, false).getMailSender();

      assertThat(impl.getJavaMailProperties())
          .containsEntry("mail.smtp.auth", false)
          .containsEntry("mail.smtp.starttls.enable", false);
    }
  }

  @Nested
  class BrandingGetUrl {

    private Branding brandingWithUrl(String url) {
      Branding branding = new Branding();
      branding.setUrl(url);
      return branding;
    }

    @Test
    void appendsTrailingSlashWhenMissing() {
      assertThat(brandingWithUrl("https://example.com").getUrl()).isEqualTo("https://example.com/");
    }

    @Test
    void keepsExistingTrailingSlash() {
      assertThat(brandingWithUrl("https://example.com/").getUrl())
          .isEqualTo("https://example.com/");
    }

    @Test
    void returnsNullUnchanged() {
      assertThat(brandingWithUrl(null).getUrl()).isNull();
    }

    @Test
    void returnsBlankUnchanged() {
      assertThat(brandingWithUrl("   ").getUrl()).isEqualTo("   ");
    }
  }

  @Nested
  class Defaults {

    @Test
    void mailSubsystemEnabledByDefault() {
      assertThat(new MailProperties().isEnabled()).isTrue();
    }

    @Test
    void perMailTypesEnabledByDefault() {
      assertThat(new MailProperties.ContactMail().isEnabled()).isTrue();
      assertThat(new MailProperties.NewUserMail().isEnabled()).isTrue();
      assertThat(new MailProperties.ResetTokenMail().isEnabled()).isTrue();
      assertThat(new MailProperties.NewLoginMail().isEnabled()).isTrue();
    }

    @Test
    void debugReceiverInactiveByDefault() {
      assertThat(new MailProperties.DebugReceiver().getActive()).isFalse();
    }
  }
}
