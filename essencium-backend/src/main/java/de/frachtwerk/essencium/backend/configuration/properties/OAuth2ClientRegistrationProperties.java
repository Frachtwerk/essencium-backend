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

import de.frachtwerk.essencium.backend.configuration.properties.embedded.UserRoleMapping;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 client registrations and providers bound from the standard Spring Security namespace
 * {@code spring.security.oauth2.client.*}.
 *
 * <p>Essencium re-binds this configuration (rather than only relying on Spring's own binding) so
 * that it can attach Essencium-specific extensions to each provider — most notably the role-mapping
 * and self-signup settings that would otherwise live under {@link
 * de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties}. The global
 * defaults in {@link
 * de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties} apply unless a
 * matching provider overrides them here.
 *
 * <p>The two maps are keyed by a free-form registration/provider id (e.g. {@code google}, {@code
 * keycloak}); a {@link Registration} references its {@link ClientProvider} via the registration's
 * {@code provider} field.
 */
@Data
@Configuration
@ConfigurationProperties("spring.security.oauth2.client")
public class OAuth2ClientRegistrationProperties {

  /**
   * Client registrations keyed by registration id (e.g. {@code google}). See {@link Registration}.
   */
  private Map<String, Registration> registration;

  /**
   * OAuth2 providers keyed by provider id, referenced by the registrations. See {@link
   * ClientProvider}.
   */
  private Map<String, ClientProvider> provider;

  /**
   * A single OAuth2 client registration (the credentials/parameters this application uses to
   * authenticate against the identity provider). Mirrors Spring's {@code ClientRegistration}
   * properties.
   */
  @EqualsAndHashCode
  @Data
  @Builder
  public static class Registration {
    /** Key of the {@link ClientProvider} in {@link #provider} this registration uses. */
    private String provider;

    /** OAuth2 client id issued by the provider. */
    private String clientId;

    /** OAuth2 client secret issued by the provider. */
    private String clientSecret;

    /**
     * Client authentication method, e.g. {@code client_secret_basic} or {@code client_secret_post}.
     */
    private String clientAuthenticationMethod;

    /** Authorization grant type, e.g. {@code authorization_code}. */
    private String authorizationGrantType;

    /** Redirect URI template the provider calls back after authentication. */
    private String redirectUri;

    /** Requested OAuth2 scopes, e.g. {@code openid}, {@code profile}, {@code email}. */
    private Set<String> scope;

    /** Human-readable client/provider name, e.g. shown on the login button. */
    private String clientName;

    /** Optional URL of an icon/logo shown for this provider on the login screen. */
    private String imageUrl;

    /**
     * Mapping of provider-specific claim names to Essencium user fields. See {@link
     * ClientRegistrationAttributes}.
     */
    private ClientRegistrationAttributes attributes;
  }

  /**
   * OAuth2 provider endpoints plus Essencium's per-provider overrides. The endpoint fields mirror
   * Spring's {@code ClientRegistration.ProviderDetails}; the trailing fields override the global
   * defaults declared in {@link
   * de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties} for this
   * provider only.
   */
  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientProvider {
    /** Provider's authorization endpoint URI. */
    private String authorizationUri;

    /** Provider's token endpoint URI. */
    private String tokenUri;

    /** Provider's user-info endpoint URI. */
    private String userInfoUri;

    /** Authentication method used when calling the user-info endpoint. */
    private String userInfoAuthenticationMethod;

    /** Name of the user-info claim that holds the unique user name/subject. */
    private String userNameAttribute;

    /** URI of the provider's JWK set (used to validate signed tokens). */
    private String jwkSetUri;

    /** Issuer URI, enabling OIDC discovery of the remaining endpoints. */
    private String issuerUri;

    /** Provider logout endpoint URI (used for single logout, if supported). */
    private String logoutUri;

    /**
     * Overrides {@link AppOAuth2Properties#isAllowSignup()} for this provider: whether unknown
     * users authenticating through it are auto-created. {@code null} = inherit the global default.
     */
    // override global properties
    private Boolean allowSignup;

    /**
     * Overrides {@link AppOAuth2Properties#isUpdateRole()} for this provider: whether the user's
     * roles are refreshed from the provider on each login. {@code null} = inherit the global
     * default.
     */
    private Boolean updateRole;

    /**
     * Overrides {@link AppOAuth2Properties#getUserRoleAttr()} for this provider: the claim carrying
     * the user's roles/groups. {@code null} = inherit the global default.
     */
    private String userRoleAttr;

    /**
     * Overrides {@link AppOAuth2Properties#getRoles()} for this provider: maps provider role/group
     * values to Essencium roles. See {@link UserRoleMapping}.
     */
    private List<UserRoleMapping> roles;
  }

  /**
   * Mapping of provider user-info claim names to Essencium user attributes ({@code
   * ...attributes.*}). Lets each provider expose its profile fields under different claim names.
   */
  @EqualsAndHashCode
  @Data
  @Builder
  public static class ClientRegistrationAttributes {
    /** Name of the claim holding the user name / login. */
    private String username;

    /** Name of the claim holding the first name. */
    private String firstname;

    /** Name of the claim holding the last name. */
    private String lastname;

    /** Name of the claim holding the full display name. */
    private String name;
  }
}
