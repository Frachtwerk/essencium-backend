package de.frachtwerk.essencium.backend.api.mocking;

import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.service.JwtTokenService;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.io.Serializable;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The MockConfig utility class can be used as entry point to configure often used mocked services
 * in a centralized manner. To chain the configuration of several mocks, the {@link
 * MockConfig#givenMocks(MockConfiguration)} and {@link MockConfig#and(MockConfiguration)} can be
 * used. Every configuration of a mocked service needs to implement the {@link MockConfiguration}
 * interface to chain the configurations.
 */
public class MockConfig {

  public static <I extends Serializable> UserRepositoryMockConfiguration<I> configure(
      BaseUserRepository<?, I> baseRepository) {

    return new UserRepositoryMockConfiguration<>(baseRepository);
  }

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

  public static JwtTokenServiceMockConfiguration configure(JwtTokenService jwtTokenService) {

    return new JwtTokenServiceMockConfiguration(jwtTokenService);
  }

  public static EnvironmentMockConfiguration configure(Environment environment) {

    return new EnvironmentMockConfiguration(environment);
  }

  public static MockConfig givenMocks(MockConfiguration configuration) {
    return new MockConfig();
  }

  public MockConfig and(MockConfiguration configuration) {
    return this;
  }
}
