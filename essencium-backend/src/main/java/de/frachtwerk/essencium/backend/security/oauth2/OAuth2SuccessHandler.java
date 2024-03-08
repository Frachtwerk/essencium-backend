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

package de.frachtwerk.essencium.backend.security.oauth2;

import de.frachtwerk.essencium.backend.configuration.properties.UserRoleMapping;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ClientRegistrationProperties;
import de.frachtwerk.essencium.backend.configuration.properties.oauth.OAuth2ConfigProperties;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.SessionTokenType;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.checked.UserEssentialsException;
import de.frachtwerk.essencium.backend.security.oauth2.util.CookieUtil;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
  private final OAuth2ConfigProperties oAuth2ConfigProperties;
  private final OAuth2ClientRegistrationProperties oAuth2ClientRegistrationProperties;

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

    Optional<String> cookieValue =
        CookieUtil.getCookieValue(request, CookieUtil.OAUTH2_REQUEST_COOKIE_NAME);
    CookieUtil.deleteCookie(request, response, CookieUtil.OAUTH2_REQUEST_COOKIE_NAME);
    if (cookieValue.isPresent() && isValidRedirectUrl(cookieValue.get())) {
      redirectHandler.setDefaultTargetUrl(cookieValue.get());
    } else if (Objects.nonNull(oAuth2ConfigProperties.getDefaultRedirectUrl())) {
      redirectHandler.setDefaultTargetUrl(oAuth2ConfigProperties.getDefaultRedirectUrl());
    }

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

      HashMap<String, Object> patch = new HashMap<>();

      patch.put("firstName", userInfo.getFirstName());
      patch.put("lastName", userInfo.getLastName());

      if (oAuth2ConfigProperties.isUpdateRole()) {
        List<Role> roles =
            extractUserRole(((OAuth2AuthenticationToken) authentication).getPrincipal());
        Role defaultRole = roleService.getDefaultRole();
        if (roles.isEmpty() && Objects.nonNull(defaultRole)) {
          LOGGER.info("no roles found for user '{}'. Using default Role.", userInfo.getUsername());
          roles.add(defaultRole);
        }
        patch.put("roles", roles);
      }

      userService.patch(Objects.requireNonNull(user.getId()), patch);

      redirectHandler.setToken(tokenService.createToken(user, SessionTokenType.ACCESS, null, null));
    } catch (UsernameNotFoundException e) {
      LOGGER.info("user {} not found locally", userInfo.getUsername());

      if (oAuth2ConfigProperties.isAllowSignup()) {
        LOGGER.info("attempting to create new user {} from successful oauth login", userInfo);

        final USER newUser = userService.createDefaultUser(userInfo, providerName);
        LOGGER.info("created new user '{}'", newUser);
        redirectHandler.setToken(
            tokenService.createToken(newUser, SessionTokenType.ACCESS, null, null));
      }
    }

    redirectHandler.onAuthenticationSuccess(request, response, authentication);
  }

  private boolean isValidRedirectUrl(String url) {
    return oAuth2ConfigProperties.getAllowedRedirectUrls().stream().anyMatch(url::equals);
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
        String firstName = (principal).getUserInfo().getGivenName();
        String lastName = (principal).getUserInfo().getFamilyName();
        if (Objects.isNull(firstName) && Objects.isNull(lastName)) {
          String[] name = StringUtils.parseFirstLastName(principal.getAttribute(OIDC_NAME_ATTR));
          userInfo.setFirstName(name[0]);
          userInfo.setLastName(name[1]);
        } else {
          userInfo.setFirstName(firstName);
          userInfo.setLastName(lastName);
        }
        userInfo.setUsername((principal).getUserInfo().getEmail());
      } else {
        userInfo.setFirstName(principal.getAttribute(OIDC_FIRST_NAME_ATTR));
        userInfo.setLastName(principal.getAttribute(OIDC_LAST_NAME_ATTR));
        userInfo.setUsername(principal.getAttribute(OIDC_EMAIL_ATTR));
      }
    } else {
      final var providerRegistration =
          oAuth2ClientRegistrationProperties.getRegistration().get(providerName);
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

      final OAuth2User principal = authentication.getPrincipal();

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

    // resolve user roles
    userInfo.setRoles(new HashSet<>(extractUserRole(authentication.getPrincipal())));

    // try fallback for first- and lastname
    userInfo.setFirstName(
        Optional.ofNullable(userInfo.getFirstName())
            .orElse(AbstractBaseUser.PLACEHOLDER_FIRST_NAME));
    userInfo.setLastName(
        Optional.ofNullable(userInfo.getLastName()).orElse(AbstractBaseUser.PLACEHOLDER_LAST_NAME));

    return userInfo;
  }

  private List<Role> extractUserRole(OAuth2User principal) {
    final var roleAttrKey = oAuth2ConfigProperties.getUserRoleAttr();
    final var roleMappings = oAuth2ConfigProperties.getRoles();
    if (roleAttrKey != null && !roleMappings.isEmpty()) {
      Collection<?> oAuthRoles =
          Optional.ofNullable(principal.getAttributes().get(roleAttrKey))
              .filter(o1 -> o1 instanceof String || o1 instanceof Collection<?>)
              .map(o1 -> o1 instanceof String ? List.of(o1) : (Collection<?>) o1)
              .orElseGet(List::of);

      return roleMappings.stream()
          .filter(m -> oAuthRoles.contains(m.getSrc()))
          .map(UserRoleMapping::getDst)
          .map(roleService::getByName)
          .collect(Collectors.toCollection(ArrayList::new));
    }
    return new ArrayList<>();
  }
}
