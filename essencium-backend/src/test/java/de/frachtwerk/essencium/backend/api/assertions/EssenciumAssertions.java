package de.frachtwerk.essencium.backend.api.assertions;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import de.frachtwerk.essencium.backend.service.RoleService;
import de.frachtwerk.essencium.backend.service.UserEmailChangeService;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.io.Serializable;
import org.springframework.security.crypto.password.PasswordEncoder;

public class EssenciumAssertions {
  public static UserAssert assertThat(UserStub userStub) {
    return new UserAssert(userStub);
  }

  public static <I extends Serializable> UserEmailChangeServiceAssert<I> assertThat(
      UserEmailChangeService<?, I> userEmailChangeService) {
    return new UserEmailChangeServiceAssert<I>(userEmailChangeService);
  }

  public static <I extends Serializable> RepositoryAssert<I> assertThat(
      BaseRepository<?, I> repository) {
    return new RepositoryAssert<I>(repository);
  }

  public static <I extends Serializable> UserRepositoryAssert<I> assertThat(
      BaseUserRepository<?, I> repository) {
    return new UserRepositoryAssert<>(repository);
  }

  public static MailAssert assertThat(UserMailService mailService) {
    return new MailAssert(mailService);
  }

  public static PasswordEncoderAssert assertThat(PasswordEncoder passwordEncoder) {
    return new PasswordEncoderAssert(passwordEncoder);
  }

  public static RoleServiceAssert assertThat(RoleService roleService) {
    return new RoleServiceAssert(roleService);
  }

  public static ExceptionAssert assertThat(Exception exception) {
    return new ExceptionAssert(exception);
  }
}
