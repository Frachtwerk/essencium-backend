package de.frachtwerk.essencium.backend.controller;

import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.dto.PasswordUpdateRequest;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ResetCredentialsControllerTest {
  @Mock AbstractUserService<?, ?, ?> userService;
  @Mock Random random;
  @InjectMocks ResetCredentialsController resetCredentialsController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testRequestResetToken() {
    when(random.nextInt(anyInt(), anyInt())).thenReturn(0);

    resetCredentialsController.requestResetToken("username");
    verify(userService).createResetPasswordToken(anyString());
  }

  @Test
  void testSetNewPassword() {
    resetCredentialsController.setNewPassword(
        new PasswordUpdateRequest("password", "verification"));
    verify(userService).resetPasswordByToken(anyString(), anyString());
  }

  @Test
  void testHandleUsernameNotFoundException() {
    resetCredentialsController.handleUsernameNotFoundException();
  }

  @Test
  void testCollectionOptions() {
    ResponseEntity<?> result = resetCredentialsController.collectionOptions();
    Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testGetAllowedMethods() {
    Set<HttpMethod> result = resetCredentialsController.getAllowedMethods();
    Assertions.assertEquals(Set.of(null), result);
  }
}

// Generated with love by TestMe :) Please raise issues & feature requests at:
// https://weirddev.com/forum#!/testme
