package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.TestUUIDUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UUIDUserService extends AbstractUserService<TestUUIDUser, UUID, UserDto<UUID>> {

  protected <T extends RoleService> UUIDUserService(
      @NotNull BaseUserRepository<TestUUIDUser, UUID> userRepository,
      @NotNull PasswordEncoder passwordEncoder,
      @NotNull UserMailService userMailService,
      @NotNull T roleService) {
    super(userRepository, passwordEncoder, userMailService, roleService);
  }

  @Override
  protected @NotNull <E extends UserDto<UUID>> TestUUIDUser convertDtoToEntity(@NotNull E entity) {

    Role role = roleService.getById(entity.getRole());
    return TestUUIDUser.builder()
        .email(entity.getEmail())
        .enabled(entity.isEnabled())
        .role(role)
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .locale(entity.getLocale())
        .mobile(entity.getMobile())
        .phone(entity.getPhone())
        .source(entity.getSource())
        .id(entity.getId())
        .build();
  }

  @Override
  public UserDto<UUID> getNewUser() {
    return new UserDto<>();
  }
}
