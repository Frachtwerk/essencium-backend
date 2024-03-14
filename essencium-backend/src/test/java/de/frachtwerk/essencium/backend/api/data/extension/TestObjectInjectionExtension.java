package de.frachtwerk.essencium.backend.api.data.extension;

import static java.lang.String.format;

import de.frachtwerk.essencium.backend.api.annotations.TestPrincipal;
import de.frachtwerk.essencium.backend.api.annotations.TestUserStub;
import de.frachtwerk.essencium.backend.api.data.TestObjects;
import de.frachtwerk.essencium.backend.api.data.user.UserStub;
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import java.lang.reflect.Parameter;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class TestObjectInjectionExtension implements ParameterResolver {
  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();

    return List.of(
            UserDto.class,
            UserStub.class,
            UsernamePasswordAuthenticationToken.class,
            Pageable.class,
            Page.class)
        .contains(parameter.getType());
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();

    if (parameter.getType().equals(UserDto.class)) {
      return TestObjects.users().userDtoBuilder().buildDefaultUserDto();
    }
    if (parameter.getType().equals(UserStub.class)) {
      return resolveUserStub(parameter);
    }
    if (parameter.getType().equals(UsernamePasswordAuthenticationToken.class)) {
      return resolveUsernamePasswordAuthenticationToken(parameter);
    }
    if (parameter.getType().equals(Page.class)) {
      return TestObjects.pageable().mockedPage();
    }
    if (parameter.getType().equals(Pageable.class)) {
      return TestObjects.pageable().mockedPageable();
    }

    throw new ParameterResolutionException(
        format("No concrete implementation found for type %s", parameter.getType()));
  }

  private Object resolveUsernamePasswordAuthenticationToken(Parameter parameter) {
    TestPrincipal parameterAnnotation = parameter.getAnnotation(TestPrincipal.class);

    if (parameterAnnotation == null || parameterAnnotation.type() == null) {
      return TestObjects.authentication().defaultLoggedInPrincipal();
    }

    switch (parameterAnnotation.type()) {
      case LOGGED_IN -> {
        return TestObjects.authentication().defaultLoggedInPrincipal();
      }
      case NOT_LOGGED_IN -> {
        return TestObjects.authentication().notLoggedInPrincipal();
      }
      case EXTERNAL_LOGGED_IN -> {
        return TestObjects.authentication().externalLoggedInPrincipal();
      }
      default ->
          throw new ParameterResolutionException(
              format("Could not find a Principal type for %s", parameterAnnotation.type()));
    }
  }

  private Object resolveUserStub(Parameter parameter) {
    TestUserStub parameterAnnotation = parameter.getAnnotation(TestUserStub.class);

    if (parameterAnnotation == null || parameterAnnotation.type() == null) {
      return TestObjects.users().defaultUser();
    }

    switch (parameterAnnotation.type()) {
      case DEFAULT -> {
        return TestObjects.users().defaultUser();
      }
      case EXTERNAL -> {
        return TestObjects.users().external();
      }
      default ->
          throw new ParameterResolutionException(
              format("Could not find a UserStub type for %s", parameterAnnotation.type()));
    }
  }
}
