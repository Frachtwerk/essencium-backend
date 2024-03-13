package de.frachtwerk.essencium.backend.api.assertions;

import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.service.UserMailService;

public class EssenciumAssertions {
  public static UserAssert assertThat(UserStub userStub) {
    return new UserAssert(userStub);
  }

  public static RepositoryAssert assertThat(BaseRepository<?, ?> repository) {
    return new RepositoryAssert(repository);
  }

  public static MailAssert assertThat(UserMailService mailService) {
    return new MailAssert(mailService);
  }
}
