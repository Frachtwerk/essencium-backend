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

import de.frachtwerk.essencium.backend.model.*;
import de.frachtwerk.essencium.backend.model.dto.ApiTokenUserDto;
import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.model.exception.*;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.model.representation.ApiTokenUserRepresentation;
import de.frachtwerk.essencium.backend.repository.ApiTokenUserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
  private final ApiTokenUserRepository apiTokenUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMailService userMailService;
  protected final RoleService roleService;
  protected final RightService rightService;
  private final JwtTokenService jwtTokenService;

  @Autowired
  protected AbstractUserService(
      @NotNull final BaseUserRepository<USER, ID> userRepository,
      @NotNull final ApiTokenUserRepository apiTokenUserRepository,
      @NotNull final PasswordEncoder passwordEncoder,
      @NotNull final UserMailService userMailService,
      @NotNull final RoleService roleService,
      @NotNull final RightService rightService,
      @NotNull final JwtTokenService jwtTokenService) {
    super(userRepository);
    this.userRepository = userRepository;
    this.apiTokenUserRepository = apiTokenUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.userMailService = userMailService;
    this.roleService = roleService;
    this.rightService = rightService;
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
    try {
      userMailService.sendResetToken(username, token, user.getLocale());
    } catch (CheckedMailException e) {
      LOG.error("Failed to send password token mail due to: {}", e.getLocalizedMessage());
    }
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

  protected void sendResetToken(USER user) throws CheckedMailException {
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
    var userToCreate = convertDtoToEntity(dto);
    userToCreate.setEmail(dto.getEmail() != null ? dto.getEmail().toLowerCase() : null);
    final String userPassword;

    if (userToCreate.hasLocalAuthentication()) {
      if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
        var passwordBytes = new byte[128];
        var token = UUID.randomUUID().toString();
        SECURE_RANDOM.nextBytes(passwordBytes);
        var randomPassword = Base64.getEncoder().encodeToString(passwordBytes);
        userToCreate.setPasswordResetToken(token);
        userPassword = randomPassword;
      } else {
        userPassword = dto.getPassword();
      }

      sanitizePassword(userToCreate, userPassword);
    }

    userToCreate.setNonce(generateNonce());
    userToCreate.setRoles(resolveRole(dto));

    return userToCreate;
  }

  @Override
  protected @NotNull USER createPostProcessing(@NotNull USER saved) {
    try {
      sendResetToken(saved);
    } catch (CheckedMailException e) {
      LOG.error("Failed to send password token mail due to: {}", e.getLocalizedMessage());
    }
    return super.createPostProcessing(saved);
  }

  @Override
  protected <E extends USERDTO> @NotNull USER updatePreProcessing(@NotNull ID id, @NotNull E dto) {
    USER existingUser =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("user does not exists"));

    var userToUpdate = super.updatePreProcessing(id, dto);
    userToUpdate.setRoles(resolveRole(dto));
    userToUpdate.setSource(existingUser.getSource());
    userToUpdate.setRoles(resolveRole(dto));
    userToUpdate.setSource(existingUser.getSource());

    sanitizePassword(userToUpdate, dto.getPassword());

    Set<Role> roles = userToUpdate.getRoles();
    userToUpdate.setRoles(new HashSet<>());
    userToUpdate = repository.save(userToUpdate);
    userToUpdate.setRoles(roles);

    if (!Objects.equals(existingUser.getEmail(), userToUpdate.getEmail())) {
      deleteAllApiTokens(id);
    }

    return userToUpdate;
  }

  @Override
  protected USER updatePostProcessing(USER saved) {
    updateApiTokens(saved);
    return super.updatePostProcessing(saved);
  }

  @Override
  protected USER patchPostProcessing(USER saved) {
    updateApiTokens(saved);
    return super.patchPostProcessing(saved);
  }

  @Override
  protected abstract <E extends USERDTO> @NotNull USER convertDtoToEntity(@NotNull E entity);

  @Override
  protected @NotNull USER patchPreProcessing(
      @NotNull ID id, @NotNull Map<String, Object> fieldUpdates) {
    final var updates = new HashMap<>(fieldUpdates); // make sure map is mutable
    Optional.ofNullable(fieldUpdates.get("roles"))
        .ifPresent(
            o -> {
              if (!(o instanceof Collection<?>)) {
                throw new IllegalArgumentException("roles must be a list");
              }
              Collection<Object> objectList = (Collection<Object>) o;
              if (objectList.isEmpty()) {
                updates.put("roles", Collections.emptySet());
              } else if (objectList.iterator().next() instanceof String) {
                updates.put(
                    "roles",
                    objectList.stream()
                        .map(String.class::cast)
                        .map(roleService::getByName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
              } else if (objectList.iterator().next() instanceof Role) {
                updates.put(
                    "roles", objectList.stream().map(Role.class::cast).collect(Collectors.toSet()));
              }
            });

    if (fieldUpdates.containsKey("email")) {
      deleteAllApiTokens(id);
    }
    var userToUpdate = super.patchPreProcessing(id, updates);

    sanitizePassword(
        userToUpdate,
        Optional.ofNullable(updates.get("password")).map(Object::toString).orElse(null));

    Set<Role> roles = userToUpdate.getRoles();
    userToUpdate.setRoles(new HashSet<>());
    userToUpdate = repository.save(userToUpdate);
    userToUpdate.setRoles(roles);

    return userToUpdate;
  }

  protected Set<Role> resolveRole(USERDTO dto) throws ResourceNotFoundException {
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

  @SuppressWarnings("unchecked")
  private USER principalAsUser(Principal principal) {
    // due to the way our authentication works we can always assume that, if a user is logged in
    // the principal is always a UsernamePasswordAuthenticationToken and the contained entity is
    // always a User as resolved by this user details service

    if (principal instanceof UsernamePasswordAuthenticationToken token
        && token.getPrincipal() instanceof AbstractBaseUser<?>) return (USER) token.getPrincipal();
    else throw new SessionAuthenticationException("not logged in");
  }

  public List<SessionToken> getTokens(USER user) {
    return jwtTokenService.getTokens(user.getUsername());
  }

  public void deleteToken(USER user, @NotNull UUID id) {
    jwtTokenService.deleteToken(user.getUsername(), id);
  }

  public ApiTokenUserRepresentation createApiToken(USER authenticatedUser, ApiTokenUserDto dto) {
    if (apiTokenUserRepository.existsByUserAndDescription(
        authenticatedUser.getUsername(), dto.getDescription())) {
      throw new InvalidInputException("A token with this description already exists");
    }
    HashSet<Right> rights =
        dto.getRights().stream()
            .map(rightService::findByAuthority)
            .filter(right -> authenticatedUser.getAuthorities().contains(right))
            .filter(right -> !right.getAuthority().startsWith("USER_TOKEN"))
            .collect(Collectors.toCollection(HashSet::new));

    if (rights.isEmpty()) {
      throw new InvalidInputException("At least one right must be selected");
    }
    ApiTokenUser apiTokenUser =
        ApiTokenUser.builder()
            .user(authenticatedUser.getUsername())
            .description(dto.getDescription())
            .rights(rights)
            .validUntil(dto.getValidUntil())
            .build();

    ApiTokenUser save = apiTokenUserRepository.save(apiTokenUser);

    String token =
        jwtTokenService.createToken(
            apiTokenUser, SessionTokenType.API, null, null, dto.getValidUntil());

    return ApiTokenUserRepresentation.builder()
        .id(save.getId())
        .description(save.getDescription())
        .rights(save.getRights())
        .user(save.getUser())
        .createdAt(save.getCreatedAt())
        .validUntil(save.getValidUntil())
        .disabled(save.isDisabled())
        .token(token)
        .build();
  }

  public Page<ApiTokenUserRepresentation> getApiTokens(
      Specification<ApiTokenUser> specification, @NotNull Pageable pageable) {
    return apiTokenUserRepository
        .findAll(specification, pageable)
        .map(
            apiTokenUser ->
                ApiTokenUserRepresentation.builder()
                    .id(apiTokenUser.getId())
                    .description(apiTokenUser.getDescription())
                    .rights(apiTokenUser.getRights())
                    .user(apiTokenUser.getUser())
                    .createdAt(apiTokenUser.getCreatedAt())
                    .validUntil(apiTokenUser.getValidUntil())
                    .disabled(apiTokenUser.isDisabled())
                    .build());
  }

  public void deleteApiToken(USER authenticatedUser, UUID id) {
    ApiTokenUser apiTokenUser =
        apiTokenUserRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTokenUser not found"));
    if (!apiTokenUser.getUser().equals(authenticatedUser.getUsername())) {
      throw new NotAllowedException("You are not allowed to disable this token");
    }
    apiTokenUserRepository.delete(apiTokenUser);
  }

  private void updateApiTokens(USER saved) {
    List<ApiTokenUser> apiTokenUsers = apiTokenUserRepository.findByUser(saved.getUsername());
    apiTokenUsers.forEach(
        apiTokenUser -> {
          apiTokenUser.getRights().removeIf(right -> !saved.getAuthorities().contains(right));
          if (apiTokenUser.getRights().isEmpty()) {
            apiTokenUserRepository.delete(apiTokenUser);
          } else {
            apiTokenUserRepository.save(apiTokenUser);
          }
        });
  }

  private void deleteAllApiTokens(ID id) {
    USER user = getById(id);
    apiTokenUserRepository.deleteAll(apiTokenUserRepository.findByUser(user.getUsername()));
  }
}
