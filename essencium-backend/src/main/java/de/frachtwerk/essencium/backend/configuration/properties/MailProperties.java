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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.validation.annotation.Validated;

/**
 * E-mail (SMTP) configuration bound from the {@code mail.*} namespace.
 *
 * <p>These properties configure the outbound {@link JavaMailSender} (host, port, credentials, TLS)
 * as well as the individual transactional mails Essencium sends (new-user, password-reset,
 * new-login and contact mails) and their shared branding. The nested {@code enabled} flags on the
 * individual mail types let single message categories be switched off independently of the global
 * SMTP connection.
 *
 * <p>Correlations: {@link #host}, {@link #port}, {@link #username}, {@link #password} and {@link
 * SMTP} together define the transport used by every mail type; the per-mail {@code template} and
 * {@code resetLink} values reference FreeMarker templates and the application base URL ({@code
 * app.url}, see {@link AppProperties}). {@link DebugReceiver}, when active, redirects/copies mail
 * to a single address for debugging.
 */
@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

  private static final String SMTP_PROTOCOL = "smtp";

  /**
   * Master switch for the mail subsystem. Default: {@code true}. When {@code false}, mail sending
   * is disabled application-wide regardless of the per-mail {@code enabled} flags.
   */
  private boolean enabled = true;

  /** SMTP server host name used by the {@link JavaMailSender}. No default; must be supplied. */
  private String host;

  /** SMTP server port (e.g. {@code 25}, {@code 465}, {@code 587}). No default; must be supplied. */
  private Integer port;

  /** User name for SMTP authentication. Only relevant when {@link SMTP#auth} is {@code true}. */
  private String username;

  /** Password for SMTP authentication. Only relevant when {@link SMTP#auth} is {@code true}. */
  private String password;

  /** Default {@code From} sender (display name and address) applied to outgoing mails. */
  private DefaultSender defaultSender;

  /** Configuration of the contact-form mail. See {@link ContactMail}. */
  private ContactMail contactMail;

  /** Configuration of the mail sent when a new user account is created. See {@link NewUserMail}. */
  private NewUserMail newUserMail;

  /** Configuration of the password-reset (reset-token) mail. See {@link ResetTokenMail}. */
  private ResetTokenMail resetTokenMail;

  /** Configuration of the notification mail sent on a new login. See {@link NewLoginMail}. */
  private NewLoginMail newLoginMail;

  /** Visual branding (logo, colors, links) shared by all mail templates. See {@link Branding}. */
  private Branding branding;

  /** SMTP transport flags (authentication, STARTTLS). See {@link SMTP}. */
  private SMTP smtp;

  /**
   * Optional debug receiver used to intercept outgoing mail during development. See {@link
   * DebugReceiver}.
   */
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

  /** Default {@code From} identity ({@code mail.default-sender.*}) used for outgoing mails. */
  @Data
  public static class DefaultSender {
    /** Display name shown as the sender, e.g. {@code "Essencium"}. */
    private String name;

    /** E-mail address used in the {@code From} header. */
    private String address;
  }

  /**
   * Debug receiver ({@code mail.debug-receiver.*}). When {@link #active} is {@code true}, outgoing
   * mail is (additionally) delivered to a single fixed {@link #address}, which is useful for
   * testing without mailing real recipients.
   */
  @Data
  @Configuration
  @ConfigurationProperties(prefix = "mail.debug-receiver")
  public static class DebugReceiver {
    /**
     * Address that receives the intercepted debug mail. Only used when {@link #active} is {@code
     * true}.
     */
    private String address;

    /** Whether the debug receiver is active. Default: {@code false}. */
    private Boolean active = false;
  }

  /** SMTP transport flags ({@code mail.smtp.*}) passed to the {@link JavaMailSender}. */
  @Data
  public static class SMTP {
    /**
     * Enables SMTP authentication ({@code mail.smtp.auth}). When {@code true}, {@link
     * MailProperties#username} and {@link MailProperties#password} are used.
     */
    Boolean auth;

    /** Enables STARTTLS ({@code mail.smtp.starttls.enable}) to upgrade the connection to TLS. */
    Boolean startTls;
  }

  @Bean
  Branding getBrandingConfig() {
    return branding;
  }

  /**
   * Visual branding ({@code mail.branding.*}) injected into all mail templates so that the sent
   * mails match the product's appearance.
   */
  @Data
  public static class Branding {
    /** URL or path of the logo image displayed in the mail header. */
    private String logo;

    /** Product / organization name shown in the mails. */
    private String name;

    /**
     * Base URL the branding (e.g. the logo) links to. A trailing slash is enforced by {@link
     * #getUrl()}.
     */
    private String url;

    /** Primary brand color (e.g. hex code) used for accents such as buttons in the templates. */
    private String primaryColor;

    /** Text color (e.g. hex code) used for the body text in the templates. */
    private String textColor;

    /** Returns {@link #url} with a guaranteed trailing slash. */
    public String getUrl() {
      return StringUtils.isBlank(url) || url.endsWith("/") ? url : url + "/";
    }
  }

  @Bean
  ContactMail getContactMailConfig() {
    return contactMail;
  }

  /**
   * Configuration of the contact-form mail ({@code mail.contact-mail.*}) that is sent to a fixed
   * set of recipients when a user submits the contact form. Correlates with {@code
   * essencium.overrides.contact-controller} ({@link EssenciumOverrideProperties}), the endpoint
   * producing these mails.
   */
  @Data
  public static class ContactMail {
    /** Whether contact mails are sent. Default: {@code true}. */
    private boolean enabled = true;

    /** Name of the FreeMarker template used to render the contact mail. */
    private String template;

    /** Recipients that receive submitted contact messages (e.g. a support inbox). */
    private Set<String> recipients;

    /**
     * Locale controlling the language of the contact mail's static structure (labels, headings).
     * Does not affect the user-provided message text. When unset, the application default locale is
     * used.
     */
    private Locale locale;

    /** Translation key whose value is prefixed to the mail subject. */
    private String subjectPrefixKey;
  }

  @Bean
  public NewUserMail getNewUserMailConfig() {
    return newUserMail;
  }

  /**
   * Configuration of the account-activation mail ({@code mail.new-user-mail.*}) sent when a new
   * user is created. It contains a link ({@link #resetLink}) that lets the user set an initial
   * password.
   */
  @Data
  public static class NewUserMail {
    /** Whether new-user mails are sent. Default: {@code true}. */
    private boolean enabled = true;

    /**
     * Translation key for the mail subject. The {@code ^[^$].*} pattern forbids a leading {@code $}
     * (which would be interpreted as a raw/placeholder value rather than a translation key).
     */
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    /** Name of the FreeMarker template used to render the mail. Mandatory ({@link NotEmpty}). */
    @NotNull @NotEmpty private String template;

    /**
     * Path/URL fragment (appended to the application base URL) at which the user sets the initial
     * password. Mandatory ({@link NotEmpty}). Correlates with {@code app.url} ({@link
     * AppProperties}).
     */
    @NotNull @NotEmpty private String resetLink;
  }

  @Bean
  ResetTokenMail getResetTokenMailConfig() {
    return resetTokenMail;
  }

  /**
   * Configuration of the password-reset mail ({@code mail.reset-token-mail.*}) sent when a user
   * requests a password reset. Structurally identical to {@link NewUserMail} but triggered by an
   * explicit reset request instead of account creation.
   */
  @Data
  public static class ResetTokenMail {
    /** Whether password-reset mails are sent. Default: {@code true}. */
    private boolean enabled = true;

    /**
     * Translation key for the mail subject. The {@code ^[^$].*} pattern forbids a leading {@code $}
     * (which would be interpreted as a raw/placeholder value rather than a translation key).
     */
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    /** Name of the FreeMarker template used to render the mail. Mandatory ({@link NotEmpty}). */
    @NotNull @NotEmpty private String template;

    /**
     * Path/URL fragment (appended to the application base URL) at which the user sets a new
     * password. Mandatory ({@link NotEmpty}). Correlates with {@code app.url} ({@link
     * AppProperties}).
     */
    @NotNull @NotEmpty private String resetLink;
  }

  @Bean
  NewLoginMail getNewLoginMailConfig() {
    return newLoginMail;
  }

  /**
   * Configuration of the new-login notification mail ({@code mail.new-login-mail.*}) that informs a
   * user about a login (e.g. from a new device). Unlike the other mail types it has no reset link.
   */
  @Data
  public static class NewLoginMail {
    /** Whether new-login notification mails are sent. Default: {@code true}. */
    private boolean enabled = true;

    /**
     * Translation key for the mail subject. The {@code ^[^$].*} pattern forbids a leading {@code $}
     * (which would be interpreted as a raw/placeholder value rather than a translation key).
     */
    @Pattern(regexp = "^[^$].*")
    private String subjectKey;

    /** Name of the FreeMarker template used to render the mail. Mandatory ({@link NotEmpty}). */
    @NotNull @NotEmpty private String template;
  }
}
