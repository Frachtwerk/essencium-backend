package de.frachtwerk.essencium.backend.api.mocking;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.io.Serializable;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MockConfig {
  public static <I extends Serializable> BaseRepositoryMockConfiguration<I> configure(
      BaseRepository<?, I> baseRepository) {

    return new BaseRepositoryMockConfiguration<>(baseRepository);
  }

  public static PasswordEncoderMockConfiguration configure(PasswordEncoder passwordEncoder) {

    return new PasswordEncoderMockConfiguration(passwordEncoder);
  }

  public static RoleServiceMockConfiguration configure(RoleService roleService) {

    return new RoleServiceMockConfiguration(roleService);
  }

  public static MailServiceMockConfiguration configure(UserMailService userMailService) {

    return new MailServiceMockConfiguration(userMailService);
  }

  public static MockConfig givenMocks(MockConfiguration configuration) {
    return new MockConfig();
  }

  public MockConfig and(MockConfiguration configuration) {
    return this;
  }
}
