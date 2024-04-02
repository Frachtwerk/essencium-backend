package de.frachtwerk.essencium.backend.api.mocking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import de.frachtwerk.essencium.backend.model.exception.checked.CheckedMailException;
import de.frachtwerk.essencium.backend.service.UserMailService;
import java.util.Set;

public class MailServiceMockConfiguration implements MockConfiguration {

  private final UserMailService mockedObject;

  public MailServiceMockConfiguration(UserMailService mockedObject) {
    this.mockedObject = mockedObject;
  }

  public MailServiceMockConfiguration trackNewUserMailSend() {
    try {
      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                MockedMetricStore.getInstance().storeSentMailWithParam(mail, Set.of(token));

                return "";
              })
          .when(mockedObject)
          .sendNewUserMail(anyString(), anyString(), any());
    } catch (CheckedMailException e) {
      throw new RuntimeException(e);
    }

    return this;
  }

  public MailServiceMockConfiguration trackResetTokenSend() {
    try {
      doAnswer(
              invocationOnMock -> {
                final String mail = invocationOnMock.getArgument(0);
                final String token = invocationOnMock.getArgument(1);

                MockedMetricStore.getInstance().storeSentMailWithParam(mail, Set.of(token));

                return "";
              })
          .when(mockedObject)
          .sendResetToken(anyString(), anyString(), any());
    } catch (CheckedMailException e) {
      throw new RuntimeException(e);
    }

    return this;
  }
}
