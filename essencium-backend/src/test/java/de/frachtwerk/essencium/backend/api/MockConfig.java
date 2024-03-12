package de.frachtwerk.essencium.backend.api;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import de.frachtwerk.essencium.backend.service.RoleService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MockConfig {

  public static MockConfigAddition useMocking() {
    return new MockConfigAddition();
  }

  public static class MockConfigAddition {
    public MockConfigAddition returnPassedEntityOnSaveFor(BaseRepository<?, ?> baseRepository) {
      doAnswer(returnsFirstArg()).when(baseRepository).save(any());
      return this;
    }

    public MockConfigAddition returnTestObjectsDefaultRoleOnDefaultRoleFor(
        RoleService roleService) {
      doReturn(TestObjects.roles().defaultRole()).when(roleService).getDefaultRole();
      return this;
    }

    public MockConfigAddition createAndReturnNewRoleOnEveryGetByNameFor(RoleService roleService) {
      doAnswer(
              invocation -> {
                final String name = invocation.getArgument(0);
                return TestObjects.roles().roleWithNameAndDescription(name);
              })
          .when(roleService)
          .getByName(anyString());
      return this;
    }

    public MockConfigAddition returnEncodedPasswordFor(
        PasswordEncoder passwordEncoder, String password, String encodedPassword) {
      doReturn(encodedPassword).when(passwordEncoder).encode(password);
      return this;
    }
  }
}
