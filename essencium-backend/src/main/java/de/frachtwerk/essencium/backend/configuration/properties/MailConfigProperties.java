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

package de.frachtwerk.essencium.backend.configuration.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "mail")
public class MailConfigProperties {

  private static final String SMTP_PROTOCOL = "smtp";

  private String host;
  private Integer port;
  private String username;
  private String password;
  private DefaultSender defaultSender;
  private ContactMail contactMail;
  private NewUserMail newUserMail;
  private ResetTokenMail resetTokenMail;
  private NewLoginMail newLoginMail;
  private Branding branding;
  private SMTP smtp;
  private DebugReceiver debugReceiver;

  @Bean
  JavaMailSender getMailSender() {
    var properties = new Properties();

    properties.put("mail.smtp.auth", smtp.auth);
    properties.put("mail.smtp.starttls.enable", smtp.startTls);

    var sender = new JavaMailSenderImpl();
    sender.setJavaMailProperties(properties);
    sender.setHost(this.host);
    sender.setPort(this.port);
    sender.setUsername(this.username);
    sender.setPassword(this.password);
    sender.setProtocol(SMTP_PROTOCOL);

    return sender;
  }

  @Bean
  DefaultSender getDefaultSenderConfig() {
    return defaultSender;
  }

  @Data
  public static class DefaultSender {
    private String name;
    private String address;
  }

  @Data
  @Configuration
  @ConfigurationProperties(prefix = "mail.debug-receiver")
  public static class DebugReceiver {
    private String address;
    private Boolean active = false;
  }

  @Data
  public static class SMTP {
    Boolean auth;
    Boolean startTls;
  }

  @Bean
  Branding getBrandingConfig() {
    return branding;
  }

  @Data
  public static class Branding {
    private String logo;
    private String name;
    private String url;
    private String primaryColor;
    private String textColor;

    public String getUrl() {
      return url.endsWith("/") ? url : url + "/";
    }
  }

  @Bean
  ContactMail getContactMailConfig() {
    return contactMail;
  }

  @Data
  public static class ContactMail {
    private String template;
    private Set<String> recipients;
    // You can set the locale to get the contact mail structure in a preferred language. This will
    // not affect the user message.
    private Locale locale;
    private String subjectPrefixKey;
  }

  @Bean
  public NewUserMail getNewUserMailConfig() {
    return newUserMail;
  }

  @Data
  public static class NewUserMail {
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    @NotNull @NotEmpty private String template;

    @NotNull @NotEmpty private String resetLink;
  }

  @Bean
  ResetTokenMail getResetTokenMailConfig() {
    return resetTokenMail;
  }

  @Data
  public static class ResetTokenMail {
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    @NotNull @NotEmpty private String template;

    @NotNull @NotEmpty private String resetLink;
  }

  @Bean
  NewLoginMail getNewLoginMailConfig() {
    return newLoginMail;
  }

  @Data
  public static class NewLoginMail {
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    @NotNull @NotEmpty private String template;
  }
}
