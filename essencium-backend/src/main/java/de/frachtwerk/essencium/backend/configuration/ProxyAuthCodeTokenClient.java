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

package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.configuration.properties.ProxyConfigProperties;
import java.util.Arrays;
import java.util.Objects;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.DefaultAuthenticationStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * @see
 *     org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
 *     Configuration taken from DefaultAuthorizationCodeTokenResponseClient and adapted Basic
 *     Problem described here:
 *     https://blog.doubleslash.de/spring-oauth2-client-authorization-request-proxy-support/
 */
@Configuration
public class ProxyAuthCodeTokenClient
    implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyAuthCodeTokenClient.class);

  private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

  private final Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>>
      requestEntityConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();

  private final ProxyConfigProperties config;

  private final RestOperations restOperations;

  class ProxyCustomizer implements RestTemplateCustomizer {
    @Override
    public void customize(RestTemplate restTemplate) {
      if (Objects.isNull(config.getHost()) || Objects.isNull(config.getPort())) {
        return;
      }
      HttpHost proxy = new HttpHost(config.getHost(), config.getPort());
      HttpClient httpClient =
          HttpClientBuilder.create().setRoutePlanner(new DefaultProxyRoutePlanner(proxy)).build();
      restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
  }

  public ProxyAuthCodeTokenClient(ProxyConfigProperties proxyConfig) {
    this.config = proxyConfig;

    RestTemplate restTemplate = new RestTemplateBuilder(new ProxyCustomizer()).build();
    restTemplate.setMessageConverters(
        Arrays.asList(
            new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

    // set up proxy for RestTemplate
    final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    clientBuilder.useSystemProperties();
    if (Objects.nonNull(proxyConfig.getHost()) && Objects.nonNull(proxyConfig.getPort())) {
      clientBuilder.setProxy(new HttpHost(proxyConfig.getHost(), proxyConfig.getPort()));
    } else {
      LOG.warn("Proxy client for OAuth was loaded but proxy config is not set.");
    }

    clientBuilder.setProxyAuthenticationStrategy(new DefaultAuthenticationStrategy());

    final CloseableHttpClient client = clientBuilder.build();
    final HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory();
    factory.setHttpClient(client);

    restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(factory));

    this.restOperations = restTemplate;
  }

  @Override
  public OAuth2AccessTokenResponse getTokenResponse(
      OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
    Assert.notNull(authorizationCodeGrantRequest, "authorizationCodeGrantRequest cannot be null");
    RequestEntity<?> request = this.requestEntityConverter.convert(authorizationCodeGrantRequest);
    ResponseEntity<OAuth2AccessTokenResponse> response = getResponse(request);
    OAuth2AccessTokenResponse tokenResponse = response.getBody();
    if (tokenResponse != null
        && tokenResponse.getAccessToken() != null
        && CollectionUtils.isEmpty(tokenResponse.getAccessToken().getScopes())) {
      // As per spec, in Section 5.1 Successful Access Token Response
      // https://tools.ietf.org/html/rfc6749#section-5.1
      // If AccessTokenResponse.scope is empty, then default to the scope
      // originally requested by the client in the Token Request
      // @formatter:off
      tokenResponse =
          OAuth2AccessTokenResponse.withResponse(tokenResponse)
              .scopes(authorizationCodeGrantRequest.getClientRegistration().getScopes())
              .build();
      // @formatter:on
    }
    return tokenResponse;
  }

  private ResponseEntity<OAuth2AccessTokenResponse> getResponse(RequestEntity<?> request) {
    try {
      return this.restOperations.exchange(request, OAuth2AccessTokenResponse.class);
    } catch (RestClientException ex) {
      OAuth2Error oauth2Error =
          new OAuth2Error(
              INVALID_TOKEN_RESPONSE_ERROR_CODE,
              "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: "
                  + ex.getMessage(),
              null);
      throw new OAuth2AuthorizationException(oauth2Error, ex);
    }
  }
}
