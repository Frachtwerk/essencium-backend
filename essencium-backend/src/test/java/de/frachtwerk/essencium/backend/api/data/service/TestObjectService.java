package de.frachtwerk.essencium.backend.api.data.service;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.service.AdminRightRoleCache;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestObjectService {
  public UserServiceStub defaultUserService(
      @NotNull BaseUserRepository<UserStub, Long> userRepositoryMock,
      @NotNull PasswordEncoder passwordEncoderMock,
      @NotNull UserMailService userMailServiceMock,
      @NotNull RoleService roleServiceMock,
      @NotNull AdminRightRoleCache adminRightRoleCache,
      @NotNull JwtTokenService jwtTokenServiceMock) {
    return new UserServiceStub(
        userRepositoryMock,
        passwordEncoderMock,
        userMailServiceMock,
        roleServiceMock,
        adminRightRoleCache,
        jwtTokenServiceMock);
  }
}
