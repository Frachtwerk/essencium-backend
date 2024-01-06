/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.test.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.util.TestingUtils;
import jakarta.servlet.ServletContext;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class TranslationIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private MockMvc mockMvc;

  @Autowired TranslationRepository translationRepository;

  @Autowired private ObjectMapper objectMapper;

  private String accessToken;

  @Autowired private TestingUtils testingUtils;

  private static TestUser testUser = null;

  @BeforeEach
  public void setupSingle() throws Exception {
    if (testUser == null) {
      testUser = testingUtils.getOrCreateAdminUser();
    }
    this.accessToken = testingUtils.createAccessToken(testUser, mockMvc);
  }

  @Test
  void checkTranslationControllerExistence() {
    ServletContext servletContext = webApplicationContext.getServletContext();

    assertThat(servletContext).isNotNull().isInstanceOf(MockServletContext.class);
    assertDoesNotThrow(() -> webApplicationContext.getBean("translationController"));
  }

  @Test
  void checkGetTranslationInEnglish() throws Exception {
    Locale locale = new Locale("en", "US");

    ResultActions resultActions =
        mockMvc
            .perform(
                get("/v1/translations/" + locale)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

    String response = resultActions.andReturn().getResponse().getContentAsString();

    Map<String, Object> result = new ObjectMapper().readValue(response, HashMap.class);

    List<Translation> testList = translationRepository.findAllByLocale(new Locale("en", "US"));

    checkTranslationObject("", result, testList);
  }

  @Test
  void checkGetTranslationInGerman() throws Exception {
    Locale locale = Locale.GERMANY;

    ResultActions resultActions =
        mockMvc
            .perform(
                get("/v1/translations/" + locale)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .characterEncoding("utf-8")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

    String response = resultActions.andReturn().getResponse().getContentAsString();
    Map<String, Object> result = new ObjectMapper().readValue(response, HashMap.class);

    List<Translation> testList = translationRepository.findAllByLocale(locale);
    checkTranslationObject("", result, testList);
  }

  @Test
  void checkCreateTranslationCache() throws Exception {
    Locale locale = Locale.GERMANY;

    String keyToSet = "testKeyGerman";
    String valueToSet = "das ist ein Test";

    Translation translation = new Translation();
    translation.setKey(keyToSet);
    translation.setLocale(locale);
    translation.setValue(valueToSet);

    mockMvc
        .perform(
            post("/v1/translations")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(translation))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk());

    List<Translation> testList = translationRepository.findAllByLocale(locale);
    assertTrue(
        testList.stream()
            .anyMatch(t -> t.getKey().equals(keyToSet) && t.getValue().equals(valueToSet)));
  }

  @Test
  void checkUpdateSingleTranslation() throws Exception {
    Locale locale = Locale.GERMANY;
    String key = "form.save";
    String value = "Test Speichern Test";

    mockMvc
        .perform(
            put("/v1/translations/" + locale + "/" + key)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk());

    List<Translation> testList = translationRepository.findAllByLocale(locale);
    assertTrue(
        testList.stream().anyMatch(t -> t.getKey().equals(key) && t.getValue().equals(value)));
  }

  @Test
  void checkUpdateAllTranslations() throws Exception {
    Locale locale = Locale.GERMANY;

    Map<String, Object> translationMap = new HashMap<>();
    translationMap.put("Test Key !! !", "Test Value ! ! !");

    mockMvc
        .perform(
            put("/v1/translations/" + locale)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(translationMap))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk());

    List<Translation> testList = translationRepository.findAllByLocale(locale);
    assertThat(testList.stream().map(Translation::getKey).toList()).contains("Test Key !! !");
    assertThat(testList.stream().map(Translation::getValue).toList()).contains("Test Value ! ! !");
  }

  @Test
  void testDownloadSingleAsJson() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/en?type=json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadSingleAsProperties() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/en?type=properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/x-java-properties"))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=en.properties"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadSingleAsResourceBundle() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/en?type=resourcebundle")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/x-java-properties"))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=en.properties"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));

    mockMvc
        .perform(
            get("/v1/translations/en?type=resource-bundle")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/x-java-properties"))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=en.properties"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadSingleAsXliff() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/en?type=xliff")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/xliff+xml"))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=en.xliff"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));

    mockMvc
        .perform(
            get("/v1/translations/en?type=xlf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/xliff+xml"))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=en.xliff"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadAllAsJson() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/file?type=json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadAllAsProperties() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/file?type=properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.zip"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadAllAsResourceBundle() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/file?type=resourcebundle")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.zip"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));

    mockMvc
        .perform(
            get("/v1/translations/file?type=resource-bundle")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.zip"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  @Test
  void testDownloadAllAsXliff() throws Exception {
    mockMvc
        .perform(
            get("/v1/translations/file?type=xliff")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/xliff+xml"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.xliff"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));

    mockMvc
        .perform(
            get("/v1/translations/file?type=xlf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/xliff+xml"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translations.xliff"))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, not("0")));
  }

  private void checkTranslationObject(String prefix, Object o, List<Translation> translations) {
    final ObjectMapper om = new ObjectMapper();
    if (o instanceof Map<?, ?>) {
      Map<String, Object> part = om.convertValue(o, Map.class);
      for (String key : part.keySet()) {
        Object newO = part.get(key);
        String newPrefix = prefix;
        if (newPrefix.isEmpty()) {
          newPrefix = key;
        } else {
          newPrefix = prefix + "." + key;
        }
        checkTranslationObject(newPrefix, newO, translations);
      }
    } else {
      boolean found = false;
      for (Translation translation : translations) {
        if (translation.getKey().equals(prefix) && translation.getValue().equals(o.toString())) {
          found = true;
        }
      }
      assertThat(found).isTrue();
    }
  }
}
