package de.frachtwerk.essencium.backend.test.integration.controller.advice;

import static de.frachtwerk.essencium.backend.test.integration.util.TestingUtils.ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.frachtwerk.essencium.backend.controller.advice.RestExceptionHandler;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class RestExceptionHandlerTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private MockMvc mockMvc;

  @Autowired private TestingUtils testingUtils;

  private String accessTokenAdmin;

  @BeforeEach
  void setUp() throws Exception {
    accessTokenAdmin =
        testingUtils.createAccessToken(testingUtils.createAdminUser(), mockMvc, ADMIN_PASSWORD);
  }

  @Test
  @DisplayName("Test for the existence of a Rest Exception Handler")
  void restExceptionHandlerExistenceTest() {
    ServletContext servletContext = webApplicationContext.getServletContext();
    assertThat(servletContext).isInstanceOf(MockServletContext.class);
    assertThat(webApplicationContext.getBean("restExceptionHandler"))
        .isNotNull()
        .isInstanceOf(RestExceptionHandler.class);
  }

  @Nested
  @DisplayName("Property Reference Exception Tests")
  class PropertyReferenceExceptionTest {

    @Test
    @DisplayName("Valid sort query parameter causes no exception")
    void noPropertyReferenceExceptionTest() throws Exception {
      mockMvc
          .perform(
              get("/v1/users?sort=email").header("Authorization", "Bearer " + accessTokenAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("Invalid sort query parameter causes exception")
    void proeprtyReferenceExceptionTest() throws Exception {
      mockMvc
          .perform(
              get("/v1/users?sort=invalid").header("Authorization", "Bearer " + accessTokenAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error", is("No property 'invalid' found for type 'TestUser'")));
    }
  }
}
