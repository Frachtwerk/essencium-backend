/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.configuration.properties.auth.AppLdapProperties;
import de.frachtwerk.essencium.backend.configuration.properties.auth.AppOAuth2Properties;
import de.frachtwerk.essencium.backend.configuration.properties.embedded.UserRoleMapping;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.security.JwtAuthenticationProvider;
import de.frachtwerk.essencium.backend.security.JwtTokenAuthenticationFilter;
import de.frachtwerk.essencium.backend.security.LdapUserContextMapper;
import de.frachtwerk.essencium.backend.security.oauth2.OAuth2AuthorizationRequestRepository;
import de.frachtwerk.essencium.backend.security.oauth2.OAuth2FailureHandler;
import de.frachtwerk.essencium.backend.security.oauth2.OAuth2SuccessHandler;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig<
    USER extends AbstractBaseUser<ID>,
    AUTHUSER extends EssenciumUserDetails<ID>,
    ID extends Serializable,
    USERDTO extends UserDto<ID>> {

  private static final RequestMatcher DEFAULT_PROTECTED_URLS =
      new OrRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/v1/**"));

  private static final RequestMatcher DEFAULT_PUBLIC_URLS =
      new OrRequestMatcher(
          new NegatedRequestMatcher(DEFAULT_PROTECTED_URLS),
          PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/v1/translations/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/v1/reset-credentials/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/v1/set-password/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
          // Optionally require authentication for contact endpoint, i.e. run full filter chain to
          // provide user object if an auth header is present , but otherwise let request pass
          // anyway
          new AndRequestMatcher(
              PathPatternRequestMatcher.withDefaults().matcher("/v1/contact/**"),
              new NegatedRequestMatcher(
                  new RequestHeaderRequestMatcher(HttpHeaders.AUTHORIZATION))));

  @Bean
  public OAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
    return new OAuth2AuthorizationRequestRepository();
  }

  // Default Services
  private final AbstractUserService<USER, AUTHUSER, ID, USERDTO> userService;
  private final RoleService roleService;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final PasswordEncoder passwordEncoder;

  // Oauth associated services and parameters
  private final OAuth2SuccessHandler<USER, AUTHUSER, ID, USERDTO> oAuth2SuccessHandler;
  private final OAuth2FailureHandler oAuth2FailureHandler;
  private final AppOAuth2Properties appOAuth2Properties;

  // LDAP associated services and Parameters
  private final AppLdapProperties appLdapProperties;
  // context mapper augments a ldap user with additional local user information
  // in this case it also supports creating a new local user from a successful ldap login
  private final LdapUserContextMapper<USER, AUTHUSER, ID, USERDTO> ldapContextMapper;
  private final BaseLdapPathContextSource ldapContextSource;

  @Bean
  protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .sessionManagement(
            (httpSecuritySessionManagementConfigurer ->
                httpSecuritySessionManagementConfigurer.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS)))
        .addFilterBefore(jwtTokenAuthenticationFilter(), AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            authorizationManagerRequestMatcherRegistry ->
                authorizationManagerRequestMatcherRegistry
                    .requestMatchers(DEFAULT_PUBLIC_URLS)
                    .permitAll()
                    .requestMatchers(DEFAULT_PROTECTED_URLS)
                    .authenticated())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable); // see AuthenticationController#logout()

    http.authenticationManager(authenticationManager());

    if (appOAuth2Properties.isEnabled()) {
      http.oauth2Login(
          httpSecurityOAuth2LoginConfigurer ->
              httpSecurityOAuth2LoginConfigurer
                  .successHandler(oAuth2SuccessHandler)
                  .failureHandler(oAuth2FailureHandler)
                  .authorizationEndpoint(
                      authorizationEndpointConfig ->
                          authorizationEndpointConfig.authorizationRequestRepository(
                              cookieAuthorizationRequestRepository())));
    }
    return http.build();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web ->
        web.ignoring()
            .requestMatchers(
                new AndRequestMatcher(
                    DEFAULT_PUBLIC_URLS,
                    new NegatedRequestMatcher(
                        new OrRequestMatcher(
                            PathPatternRequestMatcher.withDefaults().matcher("/oauth2/**"),
                            PathPatternRequestMatcher.withDefaults()
                                .matcher("/login/oauth2/**")))));
  }

  @Bean
  protected AuthenticationManager authenticationManager() {
    ProviderManager providerManager;
    if (appOAuth2Properties.isEnabled() && appLdapProperties.isEnabled()) {
      // both oauth2 and ldap enabled
      providerManager =
          new ProviderManager(
              daoAuthenticationProvider(),
              oAuth2LoginAuthenticationProvider(),
              oidcAuthorizationCodeAuthenticationProvider(),
              ldapAuthProvider());
    } else if (appOAuth2Properties.isEnabled()) {
      // only oauth2 enabled
      providerManager =
          new ProviderManager(
              daoAuthenticationProvider(),
              oAuth2LoginAuthenticationProvider(),
              oidcAuthorizationCodeAuthenticationProvider());
    } else if (appLdapProperties.isEnabled()) {
      // only ldap enabled
      providerManager = new ProviderManager(daoAuthenticationProvider(), ldapAuthProvider());
    } else {
      // only local login enabled
      providerManager = new ProviderManager(daoAuthenticationProvider());
    }
    providerManager.setAuthenticationEventPublisher(authenticationEventPublisher());
    return providerManager;
  }

  /** provide a DaoAuthenticationProvider for local login */
  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider() {
    DaoAuthenticationProvider daoAuthenticationProvider =
        new DaoAuthenticationProvider(userService);
    daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
    return daoAuthenticationProvider;
  }

  /** provide a JwtTokenAuthenticationFilter for authentication with JWT */
  @Bean
  protected JwtTokenAuthenticationFilter<ID> jwtTokenAuthenticationFilter() {
    // filter to extract jwt token from authorization bearer header
    // only apply for routes requiring authentication
    final JwtTokenAuthenticationFilter<ID> filter =
        new JwtTokenAuthenticationFilter<>(DEFAULT_PROTECTED_URLS);
    filter.setAuthenticationManager(new ProviderManager(jwtAuthenticationProvider()));
    filter.setAuthenticationSuccessHandler(successHandler());
    return filter;
  }

  /**
   * provider to resolve user details from a valid jwt token (which itself contains nothing but the
   * username as a subject) Provide a JwtAuthenticationProvider for token auth. Since it only
   * supports JwtAuthenticationTokens, it will only be called if a valid JWT token was previously
   * extracted by JwtTokenAuthenticationFilter and therefore, at best, only for PROTECTED_URLs.
   */
  @Bean
  protected JwtAuthenticationProvider<ID> jwtAuthenticationProvider() {
    return new JwtAuthenticationProvider<>();
  }

  @Bean
  protected FilterRegistrationBean<JwtTokenAuthenticationFilter<ID>> disableAutoRegistration(
      final JwtTokenAuthenticationFilter<ID> filter) {
    // prevent token auth filter from being registered twice
    // see https://octoperf.com/blog/2018/03/08/securing-rest-api-spring-security/#securityconfig
    final FilterRegistrationBean<JwtTokenAuthenticationFilter<ID>> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  @ConditionalOnProperty(value = "app.auth.oauth.enabled", havingValue = "true")
  public OAuth2LoginAuthenticationProvider oAuth2LoginAuthenticationProvider() {
    return new OAuth2LoginAuthenticationProvider(
        new RestClientAuthorizationCodeTokenResponseClient(), new DefaultOAuth2UserService());
  }

  @Bean
  @ConditionalOnProperty(value = "app.auth.oauth.enabled", havingValue = "true")
  public OidcAuthorizationCodeAuthenticationProvider oidcAuthorizationCodeAuthenticationProvider() {
    return new OidcAuthorizationCodeAuthenticationProvider(
        new RestClientAuthorizationCodeTokenResponseClient(), new OidcUserService());
  }

  @Bean
  @ConditionalOnProperty(value = "app.auth.ldap.enabled", havingValue = "true")
  public LdapAuthenticationProvider ldapAuthProvider() {
    LdapAuthenticationProvider provider =
        new LdapAuthenticationProvider(
            ldapBindAuthenticator(), ldapAuthoritiesPopulator(ldapContextSource));
    provider.setUserDetailsContextMapper(ldapContextMapper);
    return provider;
  }

  @Bean
  @ConditionalOnProperty(value = "app.auth.ldap.enabled", havingValue = "true")
  LdapAuthoritiesPopulator ldapAuthoritiesPopulator(BaseLdapPathContextSource contextSource) {
    DefaultLdapAuthoritiesPopulator authorities =
        new DefaultLdapAuthoritiesPopulator(contextSource, appLdapProperties.getGroupSearchBase());
    authorities.setGroupSearchFilter(appLdapProperties.getGroupSearchFilter());
    authorities.setSearchSubtree(appLdapProperties.isGroupSearchSubtree());
    authorities.setAuthorityMapper(
        item -> {
          List<String> roles = item.get(appLdapProperties.getGroupRoleAttribute());
          if (CollectionUtils.isEmpty(roles) || Objects.isNull(roles.getFirst())) {
            return null;
          }
          String appRole =
              appLdapProperties.getRoles().stream()
                  .filter(userRoleMapping -> userRoleMapping.getSrc().equals(roles.getFirst()))
                  .findFirst()
                  .map(UserRoleMapping::getDst)
                  .orElse(null);
          if (appRole == null) {
            return null;
          }
          return roleService.getByName(appRole.toUpperCase());
        });
    authorities.setDefaultRole(appLdapProperties.getDefaultRole());
    return authorities;
  }

  @Bean
  @ConditionalOnProperty(value = "app.auth.ldap.enabled", havingValue = "true")
  public BindAuthenticator ldapBindAuthenticator() {
    FilterBasedLdapUserSearch filterBasedLdapUserSearch =
        new FilterBasedLdapUserSearch(
            appLdapProperties.getUserSearchBase(),
            appLdapProperties.getUserSearchFilter(),
            ldapContextSource);
    BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource);
    authenticator.setUserSearch(filterBasedLdapUserSearch);
    return authenticator;
  }

  /** Authentication Event Handling */
  @Bean
  public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
    return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
  }

  @Bean
  protected AuthenticationSuccessHandler successHandler() {
    // do nothing after successful authentication
    // inspired by
    // https://octoperf.com/blog/2018/03/08/securing-rest-api-spring-security/#redirect-strategy
    final SimpleUrlAuthenticationSuccessHandler successHandler =
        new SimpleUrlAuthenticationSuccessHandler();
    successHandler.setRedirectStrategy((request, response, url) -> {}); // no redirect
    return successHandler;
  }
}
