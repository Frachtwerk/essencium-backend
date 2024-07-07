package de.frachtwerk.essencium.backend.api.data.service;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.configuration.properties.SecurityConfigProperties;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.security.BruteForceProtectionService;
import de.frachtwerk.essencium.backend.service.AdminRightRoleCache;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserEmailChangeService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestObjectService {

  public UserServiceStub defaultUserService(
      @NotNull BaseUserRepository<UserStub, Long> userRepositoryMock,
      @NotNull PasswordEncoder passwordEncoderMock,
      @NotNull UserMailService userMailServiceMock,
      @NotNull RoleService roleServiceMock,
      @NotNull AdminRightRoleCache adminRightRoleCache,
      @NotNull JwtTokenService jwtTokenServiceMock,
      @NotNull BruteForceProtectionService<UserStub, Long> bruteForceProtectionService,
      @NotNull Environment environment) {

    return createUserServiceStub(
        userRepositoryMock,
        passwordEncoderMock,
        userMailServiceMock,
        roleServiceMock,
        adminRightRoleCache,
        jwtTokenServiceMock,
        bruteForceProtectionService,
        environment,
        new SecurityConfigProperties());
  }

  public UserServiceStub disabledEmailVerificationUserService(
      @NotNull BaseUserRepository<UserStub, Long> userRepositoryMock,
      @NotNull PasswordEncoder passwordEncoderMock,
      @NotNull UserMailService userMailServiceMock,
      @NotNull RoleService roleServiceMock,
      @NotNull AdminRightRoleCache adminRightRoleCache,
      @NotNull JwtTokenService jwtTokenServiceMock,
      @NotNull BruteForceProtectionService<UserStub, Long> bruteForceProtectionService,
      @NotNull Environment environment) {

    SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
    securityConfigProperties.setEMailValidationDisabled(true);

    return createUserServiceStub(
        userRepositoryMock,
        passwordEncoderMock,
        userMailServiceMock,
        roleServiceMock,
        adminRightRoleCache,
        jwtTokenServiceMock,
        bruteForceProtectionService,
        environment,
        securityConfigProperties);
  }

  private UserServiceStub createUserServiceStub(
      BaseUserRepository<UserStub, Long> userRepositoryMock,
      PasswordEncoder passwordEncoderMock,
      UserMailService userMailServiceMock,
      RoleService roleServiceMock,
      AdminRightRoleCache adminRightRoleCache,
      JwtTokenService jwtTokenServiceMock,
      BruteForceProtectionService<UserStub, Long> bruteForceProtectionService,
      Environment environment,
      SecurityConfigProperties securityConfigProperties) {
    UserEmailChangeService<UserStub, Long> userEmailChangeService =
        new UserEmailChangeService<>(
            userRepositoryMock,
            userMailServiceMock,
            bruteForceProtectionService,
            securityConfigProperties);

    return new UserServiceStub(
        userRepositoryMock,
        passwordEncoderMock,
        userMailServiceMock,
        roleServiceMock,
        adminRightRoleCache,
        jwtTokenServiceMock,
        userEmailChangeService,
        environment,
        securityConfigProperties);
  }
}
