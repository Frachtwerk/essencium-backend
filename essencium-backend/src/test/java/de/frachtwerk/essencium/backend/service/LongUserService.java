package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LongUserService extends AbstractUserService<TestLongUser, Long, UserDto<Long>> {

  protected <T extends RoleService> LongUserService(
      @NotNull BaseUserRepository<TestLongUser, Long> userRepository,
      @NotNull PasswordEncoder passwordEncoder,
      @NotNull UserMailService userMailService,
      @NotNull T roleService) {
    super(userRepository, passwordEncoder, userMailService, roleService);
  }

  @Override
  protected @NotNull <E extends UserDto<Long>> TestLongUser convertDtoToEntity(@NotNull E entity) {

    Role role = roleService.getById(entity.getRole());
    return TestLongUser.builder()
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
  public UserDto<Long> getNewUser() {
    return new UserDto<>();
  }
}
