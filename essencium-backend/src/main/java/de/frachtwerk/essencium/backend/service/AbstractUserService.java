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

package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.model.AbstractBaseUser.USER_ROLE_ATTRIBUTE;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.model.UserInfoEssentials;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

public abstract class AbstractUserService<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    extends AbstractEntityService<USER, ID, USERDTO> implements UserDetailsService {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractUserService.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  protected final BaseUserRepository<USER, ID> userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMailService userMailService;
  protected final RoleService roleService;
  protected final AdminRightRoleCache adminRightRoleCache;
  private final JwtTokenService jwtTokenService;

  @Autowired
  protected AbstractUserService(
      @NotNull final BaseUserRepository<USER, ID> userRepository,
      @NotNull final PasswordEncoder passwordEncoder,
      @NotNull final UserMailService userMailService,
      @NotNull final RoleService roleService,
      @NotNull final AdminRightRoleCache adminRightRoleCache,
      @NotNull final JwtTokenService jwtTokenService) {
    super(userRepository);
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.userMailService = userMailService;
    this.roleService = roleService;
    this.adminRightRoleCache = adminRightRoleCache;
    this.jwtTokenService = jwtTokenService;
  }

  @PostConstruct
  private void setup() {
    this.roleService.setUserService(this);
    this.jwtTokenService.setUserService(this);
  }

  @Override
  public USER loadUserByUsername(final String username) throws UsernameNotFoundException {
    return userRepository
        .findByEmailIgnoreCase(username)
        .orElseThrow(
            () -> new UsernameNotFoundException(String.format("user '%s' not found", username)));
  }

  public List<USER> loadUsersByRole(final String role) throws UsernameNotFoundException {
    return userRepository.findByRoleName(role);
  }

  @NotNull
  public USER getUserFromPrincipal(@Nullable final Principal principal) {
    return principalAsUser(principal);
  }

  public void createResetPasswordToken(@NotNull final String username) {
    var user = loadUserByUsername(username);

    if (!user.hasLocalAuthentication()) {
      throw new NotAllowedException(
          String.format(
              "cannot reset password for users authenticated via '%s'", user.getSource()));
    }

    var token = createAndSaveNewPasswordToken(user);
    userMailService.sendResetToken(username, token, user.getLocale());
  }

  public void resetPasswordByToken(@NotNull final String token, @NotNull final String newPassword) {
    var userToUpdate =
        userRepository
            .findByPasswordResetToken(token)
            .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));
    setNewPasswordAndClearToken(userToUpdate, newPassword);
  }

  protected void setNewPasswordAndClearToken(
      @NotNull final USER user, @NotNull final String newPassword) {
    sanitizePassword(user, newPassword);
    user.setLoginDisabled(false);
    user.setPasswordResetToken(null);
    userRepository.save(user);
  }

  @NotNull
  protected String createAndSaveNewPasswordToken(@NotNull final USER user) {
    var resetToken = UUID.randomUUID().toString();
    user.setPasswordResetToken(resetToken);
    return userRepository.save(user).getPasswordResetToken();
  }

  protected void sendResetToken(USER user) {
    if (user.hasLocalAuthentication()) {
      var resetToken = user.getPasswordResetToken();
      if (resetToken != null && !resetToken.isEmpty()) {
        userMailService.sendNewUserMail(
            user.getEmail(), user.getPasswordResetToken(), user.getLocale());
      }
    }
  }

  public USER save(USER user) {
    return userRepository.save(user);
  }

  @NotNull
  @Override
  protected <E extends USERDTO> @NotNull USER createPreProcessing(@NotNull E dto) {
    var userToCreate = convertDtoToEntity(dto, Optional.empty());
    userToCreate.setEmail(dto.getEmail() != null ? dto.getEmail().toLowerCase() : null);
    final String userPassword;

    if (userToCreate.hasLocalAuthentication()) {
      if (dto.getPassword() == null || dto.getPassword().isBlank()) {
        var passwordBytes = new byte[128];
        var token = UUID.randomUUID().toString();
        SECURE_RANDOM.nextBytes(passwordBytes);
        String randomPassword =
            Base64.getEncoder()
                .encodeToString(passwordBytes)
                .substring(
                    0, 72); // due to a limitation in BCrypt ony the first 72 characters are used
        userToCreate.setPasswordResetToken(token);
        userPassword = randomPassword;
      } else {
        userPassword = dto.getPassword();
      }

      sanitizePassword(userToCreate, userPassword);
    }

    userToCreate.setNonce(generateNonce());
    userToCreate.setRoles(resolveRoles(dto));

    return userToCreate;
  }

  @Override
  protected @NotNull USER createPostProcessing(@NotNull USER saved) {
    sendResetToken(saved);
    return super.createPostProcessing(saved);
  }

  @Override
  protected <E extends USERDTO> @NotNull USER updatePreProcessing(@NotNull ID id, @NotNull E dto) {
    var existingUser = repository.findById(id);

    Set<Role> rolesWithinUpdate = resolveRoles(dto);

    abortWhenRemovingAdminRole(id, rolesWithinUpdate);

    var userToUpdate = super.updatePreProcessing(id, dto);
    userToUpdate.setRoles(rolesWithinUpdate);
    userToUpdate.setSource(
        existingUser
            .map(USER::getSource)
            .orElseThrow(() -> new ResourceNotFoundException("user does not exists")));

    sanitizePassword(userToUpdate, dto.getPassword());

    return userToUpdate;
  }

  @Override
  protected abstract <E extends USERDTO> @NotNull USER convertDtoToEntity(
      @NotNull E entity, Optional<USER> currentEntityOpt);

  @Override
  protected @NotNull USER patchPreProcessing(
      @NotNull ID id, @NotNull Map<String, Object> fieldUpdates) {
    final HashMap<String, Object> updates = new HashMap<>(fieldUpdates); // make sure map is mutable
    Optional.ofNullable(fieldUpdates.get(USER_ROLE_ATTRIBUTE))
        .ifPresent(
            o -> {
              if (o instanceof Collection<?> objects) {
                if (objects.isEmpty()) {
                  updates.put(USER_ROLE_ATTRIBUTE, Collections.emptySet());
                } else if (objects.iterator().next() instanceof String) {
                  updates.put(
                      USER_ROLE_ATTRIBUTE,
                      objects.stream()
                          .map(String.class::cast)
                          .map(roleService::getByName)
                          .filter(Objects::nonNull)
                          .collect(Collectors.toSet()));
                } else if (objects.iterator().next() instanceof Map) {
                  updates.put(
                      USER_ROLE_ATTRIBUTE,
                      objects.stream()
                          .map(Map.class::cast)
                          .map(roleMap -> roleMap.get("name"))
                          .filter(String.class::isInstance)
                          .map(String.class::cast)
                          .map(roleService::getByName)
                          .filter(Objects::nonNull)
                          .collect(Collectors.toSet()));
                } else if (objects.iterator().next() instanceof Role) {
                  updates.put(
                      USER_ROLE_ATTRIBUTE,
                      objects.stream()
                          .map(Role.class::cast)
                          .map(role -> roleService.getByName(role.getName()))
                          .filter(Objects::nonNull)
                          .collect(Collectors.toSet()));
                }
              } else {
                throw new IllegalArgumentException("roles must be a collection of strings or maps");
              }
            });

    if (updates.get(USER_ROLE_ATTRIBUTE) != null) {
      abortWhenRemovingAdminRole(id, (Set<Role>) updates.get(USER_ROLE_ATTRIBUTE));
    }

    var userToUpdate = super.patchPreProcessing(id, updates);

    sanitizePassword(
        userToUpdate,
        Optional.ofNullable(updates.get("password")).map(Object::toString).orElse(null));

    return userToUpdate;
  }

  protected Set<Role> resolveRoles(USERDTO dto) throws ResourceNotFoundException {
    Set<Role> roles =
        dto.getRoles().stream()
            .map(roleService::getByName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Role defaultRole = roleService.getDefaultRole();
    if (roles.isEmpty() && Objects.nonNull(defaultRole)) {
      roles.add(defaultRole);
    }
    return roles;
  }

  protected void sanitizePassword(@NotNull USER user, @Nullable String newPassword) {
    Optional<USER> existingUser =
        Optional.ofNullable(user.getId()).flatMap(userRepository::findById);
    if (newPassword != null
        && !newPassword.isEmpty()
        && existingUser.map(AbstractBaseUser::hasLocalAuthentication).orElse(true)) {
      user.setNonce(generateNonce());
      user.setPassword(passwordEncoder.encode(newPassword));
    } else {
      user.setPassword(existingUser.map(AbstractBaseUser::getPassword).orElse(null));
    }

    if (user.getNonce() == null) {
      user.setNonce(existingUser.map(AbstractBaseUser::getNonce).orElse(null));
    }
  }

  @NotNull
  public USER selfUpdate(@NotNull final USER user, @NotNull final USERDTO updateInformation) {
    user.setFirstName(updateInformation.getFirstName());
    user.setLastName(updateInformation.getLastName());
    user.setPhone(updateInformation.getPhone());
    user.setMobile(updateInformation.getMobile());
    user.setLocale(updateInformation.getLocale());

    return userRepository.save(user);
  }

  @NotNull
  public USER selfUpdate(
      @NotNull final USER user, @NotNull final Map<String, Object> updateFields) {
    final var permittedFields = Set.of("firstName", "lastName", "phone", "mobile", "locale");
    final var filteredFields =
        updateFields.entrySet().stream()
            .filter(e -> permittedFields.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return patch(Objects.requireNonNull(user.getId()), filteredFields);
  }

  @NotNull
  public USER updatePassword(
      @NotNull final USER user, @Valid @NotNull final PasswordUpdateRequest updateRequest) {
    if (!user.hasLocalAuthentication()) {
      throw new NotAllowedException(
          String.format(
              "cannot reset password for users authenticated via '%s'", user.getSource()));
    }

    if (!passwordEncoder.matches(updateRequest.verification(), user.getPassword())) {
      throw new BadCredentialsException("mismatching passwords");
    }

    sanitizePassword(user, updateRequest.password());
    return userRepository.save(user);
  }

  public abstract USERDTO getNewUser();

  public static String generateNonce() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  public USER createDefaultUser(UserInfoEssentials userInfo, String source) {
    Set<Role> roles = userInfo.getRoles();

    Role defaultRole = roleService.getDefaultRole();
    if (roles.isEmpty() && Objects.nonNull(defaultRole)) {
      roles.add(defaultRole);
    }

    final USERDTO user = getNewUser();
    user.setEmail(userInfo.getUsername().toLowerCase());
    user.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
    user.setSource(source);
    user.setLocale(AbstractBaseUser.DEFAULT_LOCALE);
    user.setFirstName(userInfo.getFirstName());
    user.setLastName(userInfo.getLastName());
    return create(user);
  }

  private USER principalAsUser(Principal principal) {
    // due to the way our authentication works we can always assume that, if a user is logged in
    // the principal is always a UsernamePasswordAuthenticationToken and the contained entity is
    // always a User as resolved by this user details service

    if (!(principal instanceof UsernamePasswordAuthenticationToken)
        || !(((UsernamePasswordAuthenticationToken) principal).getPrincipal()
            instanceof AbstractBaseUser)) {
      throw new SessionAuthenticationException("not logged in");
    }
    return (USER) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
  }

  public List<SessionToken> getTokens(USER user) {
    return jwtTokenService.getTokens(user.getUsername());
  }

  public void deleteToken(USER user, @NotNull UUID id) {
    jwtTokenService.deleteToken(user.getUsername(), id);
  }

  @Override
  protected void deletePreProcessing(@NotNull final ID id) {
    super.deletePreProcessing(id);

    userRepository.findById(id).ifPresent(user -> throwNotAllowedExceptionIfNoOtherAdminExists(id));
  }

  private void abortWhenRemovingAdminRole(ID id, Set<Role> rolesWithinUpdate) {
    boolean userRemainsAdmin =
        rolesWithinUpdate.stream().anyMatch(adminRightRoleCache.getAdminRoles()::contains);

    if (!userRemainsAdmin) {
      throwNotAllowedExceptionIfNoOtherAdminExists(id);
    }
  }

  private void throwNotAllowedExceptionIfNoOtherAdminExists(ID ignoredUserId) {
    boolean doesOtherAdminExists =
        userRepository.existsAnyAdminBesidesUserWithId(
            adminRightRoleCache.getAdminRoles(), ignoredUserId);

    if (!doesOtherAdminExists) {
      throw new NotAllowedException(
          "You cannot remove the role 'ADMIN' from yourself. That is to ensure there's at least one ADMIN remaining.");
    }
  }
}
