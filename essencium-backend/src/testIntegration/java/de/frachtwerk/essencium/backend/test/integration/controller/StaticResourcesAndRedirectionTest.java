/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import de.frachtwerk.essencium.backend.test.integration.IntegrationTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local_integration_test")
class StaticResourcesAndRedirectionTest {

  private static final String INDEX_TEXT = "It works!";
  private static final String TEST_TEXT = "Just a test";

  @Autowired private MockMvc mockMvc;

  @Test
  void testGetRoot() throws Exception {
    mockMvc.perform(get("/")).andExpect(forwardedUrl("index.html"));
  }

  @Test
  void testGetIndex() throws Exception {
    mockMvc
        .perform(get("/index.html"))
        .andExpect(content().contentType(MediaType.TEXT_HTML))
        .andExpect(content().string(INDEX_TEXT));
  }

  @Test
  void testGetExistingStatic() throws Exception {
    mockMvc
        .perform(get("/test.html"))
        .andExpect(content().contentType(MediaType.TEXT_HTML))
        .andExpect(content().string(TEST_TEXT));
  }

  @Test
  void testRedirectNonExistingStatic() throws Exception {
    mockMvc
        .perform(get("/nonexisting"))
        .andExpect(content().contentType(MediaType.TEXT_HTML))
        .andExpect(content().string(INDEX_TEXT));
  }
}
