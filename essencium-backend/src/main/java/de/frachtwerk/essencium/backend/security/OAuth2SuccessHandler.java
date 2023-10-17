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

package de.frachtwerk.essencium.backend.security;

import de.frachtwerk.essencium.backend.configuration.properties.OAuthConfigProperties;
import de.frachtwerk.essencium.backend.configuration.properties.UserRoleMapping;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.ClientProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.checked.UserEssentialsException;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.util.StringUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2SuccessHandler<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    implements AuthenticationSuccessHandler {

  public static final String OIDC_FIRST_NAME_ATTR = "given_name";
  public static final String OIDC_LAST_NAME_ATTR = "family_name";
  public static final String OIDC_NAME_ATTR = "name";
  public static final String OIDC_EMAIL_ATTR = "email";

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

  private final JwtTokenService tokenService;
  private final AbstractUserService<USER, ID, USERDTO> userService;
  private final RoleService roleService;
  private final OAuthConfigProperties oAuthConfigProperties;

  private final ClientProperties oAuthClientProperties;

  @Autowired
  public OAuth2SuccessHandler(
      JwtTokenService tokenService,
      AbstractUserService<USER, ID, USERDTO> userService,
      RoleService roleService,
      OAuthConfigProperties oAuthConfigProperties,
      ClientProperties oAuthClientProperties) {
    this.tokenService = tokenService;
    this.userService = userService;
    this.roleService = roleService;
    this.oAuthConfigProperties = oAuthConfigProperties;
    this.oAuthClientProperties = oAuthClientProperties;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    // oauth login is assumed to be only used in the browser
    // accordingly, after successful login, the user will be redirected to where
    // she came from (or front page as a fallback) with either the jwt token passed as a query param
    // or ?login_failure in case either oauth login itself failed or no matching local user was
    // found or created

    final RedirectHandler redirectHandler = new RedirectHandler();

    if (!(authentication instanceof OAuth2AuthenticationToken)) {
      LOGGER.error(
          "did not receive an instance of {}, aborting",
          OAuth2AuthenticationToken.class.getSimpleName());
      redirectHandler.onAuthenticationSuccess(request, response, authentication);
      return;
    }

    final var providerName =
        ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

    UserInfoEssentials userInfo;
    try {
      LOGGER.info(
          "attempting to log in oauth2 user '{}' using provider '{}'",
          authentication.getName(),
          providerName);
      userInfo = extractUserInfo((OAuth2AuthenticationToken) authentication, providerName);
    } catch (UserEssentialsException e) {
      LOGGER.error(e.getMessage());
      redirectHandler.onAuthenticationSuccess(request, response, authentication);
      return;
    }

    try {
      final var user = userService.loadUserByUsername(userInfo.getUsername());
      LOGGER.info("got successful oauth login for {}", userInfo.getUsername());

      if (oAuthConfigProperties.isUpdateRole()) {
        final var desiredRole =
            extractUserRole(((OAuth2AuthenticationToken) authentication).getPrincipal())
                .orElseGet(() -> roleService.getDefaultRole().orElse(null));

        if (desiredRole != null && !desiredRole.getName().equals(user.getRole().getName())) {
          LOGGER.info(
              "updating {}'s role from {} to {} based on oauth mapping",
              user.getUsername(),
              user.getRole().getName(),
              desiredRole.getName());
          user.setRole(desiredRole);
          userService.patch(
              Objects.requireNonNull(user.getId()), Map.of("role", desiredRole.getName()));
        }
      }

      redirectHandler.setToken(tokenService.createToken(user, SessionTokenType.ACCESS, null, null));
    } catch (UsernameNotFoundException e) {
      LOGGER.info("user {} not found locally", userInfo.getUsername());

      if (oAuthConfigProperties.isAllowSignup()) {
        LOGGER.info("attempting to create new user {} from successful oauth login", userInfo);

        final USER newUser = userService.createDefaultUser(userInfo, providerName);
        LOGGER.info("created new user '{}'", newUser);
        redirectHandler.setToken(
            tokenService.createToken(newUser, SessionTokenType.ACCESS, null, null));
      }
    }

    redirectHandler.onAuthenticationSuccess(request, response, authentication);
  }

  static class RedirectHandler extends SimpleUrlAuthenticationSuccessHandler {
    private String token;

    public void setToken(String token) {
      this.token = token;
    }

    @Override
    protected String determineTargetUrl(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
      String baseUrl = super.determineTargetUrl(request, response, authentication);
      if (token != null) {
        return String.format("%s?token=%s", baseUrl, token);
      }
      return String.format("%s?login_failure", baseUrl);
    }
  }

  private UserInfoEssentials extractUserInfo(
      OAuth2AuthenticationToken authentication, String providerName)
      throws UserEssentialsException {
    final UserInfoEssentials userInfo = new UserInfoEssentials();

    // ToDo: Mapping for different providers
    if (authentication.getPrincipal() instanceof final OidcUser principal) {
      if (principal.getUserInfo() != null) {
        userInfo.setFirstName((principal).getUserInfo().getGivenName());
        userInfo.setLastName((principal).getUserInfo().getGivenName());
        userInfo.setUsername((principal).getUserInfo().getEmail());
      } else {
        userInfo.setFirstName(principal.getAttribute(OIDC_FIRST_NAME_ATTR));
        userInfo.setLastName(principal.getAttribute(OIDC_LAST_NAME_ATTR));
        userInfo.setUsername(principal.getAttribute(OIDC_EMAIL_ATTR));
      }
    } else {
      final var providerRegistration = oAuthClientProperties.getRegistration().get(providerName);
      if (providerRegistration == null) {
        throw new UserEssentialsException(
            String.format("could not resolve provider registration '%s'", providerName));
      }

      final String userUsernameKey =
          Optional.ofNullable(providerRegistration.getAttributes())
              .flatMap(a -> Optional.ofNullable(a.getUsername()))
              .orElse(OIDC_EMAIL_ATTR);
      final String firstNameKey =
          Optional.ofNullable(providerRegistration.getAttributes())
              .flatMap(a -> Optional.ofNullable(a.getFirstname()))
              .orElse(OIDC_FIRST_NAME_ATTR);
      final String lastNameKey =
          Optional.ofNullable(providerRegistration.getAttributes())
              .flatMap(a -> Optional.ofNullable(a.getLastname()))
              .orElse(OIDC_LAST_NAME_ATTR);
      final String userNameKey =
          Optional.ofNullable(providerRegistration.getAttributes())
              .flatMap(a -> Optional.ofNullable(a.getName()))
              .orElse(OIDC_NAME_ATTR);

      final var principal = (OAuth2User) authentication.getPrincipal();

      userInfo.setUsername(principal.getAttribute(userUsernameKey));
      if ((!principal.getAttributes().containsKey(firstNameKey)
          || !principal.getAttributes().containsKey(lastNameKey))) {
        LOGGER.debug("attempting to parse first- and last name from combined name field");

        final var parsedName = StringUtils.parseFirstLastName(principal.getAttribute(userNameKey));
        userInfo.setFirstName(Objects.requireNonNull(parsedName)[0]);
        userInfo.setLastName(parsedName[1]);
      } else {
        userInfo.setFirstName(principal.getAttribute(firstNameKey));
        userInfo.setLastName(principal.getAttribute(lastNameKey));
      }
    }

    // try fallback for username
    if (userInfo.getUsername() == null) {
      if (StringUtils.isValidEmailAddress(authentication.getName())) {
        userInfo.setUsername(authentication.getName());
      } else {
        throw new UserEssentialsException(
            "failed to extract username from authentication information");
      }
    }

    // resolve user role
    extractUserRole(authentication.getPrincipal())
        .ifPresentOrElse(
            userInfo::setRole,
            () -> LOGGER.warn("no appropriate role found for user '{}'", userInfo.getUsername()));

    // try fallback for first- and lastname
    userInfo.setFirstName(
        Optional.ofNullable(userInfo.getFirstName())
            .orElse(AbstractBaseUser.PLACEHOLDER_FIRST_NAME));
    userInfo.setLastName(
        Optional.ofNullable(userInfo.getLastName()).orElse(AbstractBaseUser.PLACEHOLDER_LAST_NAME));

    return userInfo;
  }

  private Optional<Role> extractUserRole(OAuth2User principal) {
    final var roleAttrKey = oAuthConfigProperties.getUserRoleAttr();
    final var roleMappings = oAuthConfigProperties.getRoles();
    if (roleAttrKey != null && !roleMappings.isEmpty()) {
      Collection<?> oAuthRoles =
          Optional.ofNullable(principal.getAttributes().get(roleAttrKey))
              .filter(o1 -> o1 instanceof String || o1 instanceof Collection<?>)
              .map(o1 -> o1 instanceof String ? List.of(o1) : (Collection<?>) o1)
              .orElseGet(List::of);

      return roleMappings.stream()
          .filter(m -> oAuthRoles.contains(m.getSrc()))
          .map(UserRoleMapping::getDst)
          .map(roleService::getRole)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst();
    }
    return Optional.empty();
  }
}
