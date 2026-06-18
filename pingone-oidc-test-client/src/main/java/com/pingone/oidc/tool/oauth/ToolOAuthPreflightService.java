package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;

@Service
public class ToolOAuthPreflightService {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthPreflightService.class);
    private static final String PROBE_CODE = "pingone-tool-preflight-probe";

    private final ClientToolRegistrationFactory registrationFactory;
    private final ToolOAuthErrorMapper errorMapper;
    private final WebClient webClient;

    public ToolOAuthPreflightService(ClientToolRegistrationFactory registrationFactory, ToolOAuthErrorMapper errorMapper) {
        this.registrationFactory = registrationFactory;
        this.errorMapper = errorMapper;
        HttpClient httpClient = HttpClient.create().followRedirect(false);
        this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    public ToolOAuthLoginError validate(ClientToolConfigRequest request) {
        ClientRegistration registration = registrationFactory.build(request);
        ToolOAuthLoginError authorizationError = probeAuthorizationEndpoint(registration);
        if (authorizationError != null) {
            log.warn(
                    "Tool OAuth preflight authorization probe failed for client '{}': {}",
                    registration.getClientId(),
                    authorizationError.errorCode());
            return authorizationError;
        }

        ToolOAuthLoginError tokenError = probeTokenEndpoint(registration);
        if (tokenError != null) {
            log.warn(
                    "Tool OAuth preflight token probe failed for client '{}': {}",
                    registration.getClientId(),
                    tokenError.errorCode());
            return tokenError;
        }

        log.info("Tool OAuth preflight passed for client '{}'", registration.getClientId());
        return null;
    }

    private ToolOAuthLoginError probeAuthorizationEndpoint(ClientRegistration registration) {
        String authorizationUri = registration.getProviderDetails().getAuthorizationUri();
        if (!StringUtils.hasText(authorizationUri)) {
            return null;
        }
        String probeUrl = UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", registration.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", registration.getRedirectUri())
                .queryParam("scope", String.join(" ", registration.getScopes()))
                .queryParam("state", "pingone-tool-preflight")
                .build()
                .encode()
                .toUriString();
        try {
            webClient
                    .get()
                    .uri(probeUrl)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE + ", text/html")
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()
                                || response.statusCode().is3xxRedirection()) {
                            return response.releaseBody().thenReturn(Boolean.TRUE);
                        }
                        return response.bodyToMono(String.class).flatMap(body -> {
                            WebClientResponseException ex = WebClientResponseException.create(
                                    response.statusCode().value(),
                                    response.statusCode().toString(),
                                    response.headers().asHttpHeaders(),
                                    body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0],
                                    StandardCharsets.UTF_8);
                            return reactor.core.publisher.Mono.error(ex);
                        });
                    })
                    .block();
            return null;
        } catch (WebClientResponseException ex) {
            return errorMapper.mapPreflight(ex);
        } catch (Exception ex) {
            return errorMapper.mapPreflight("authorization_probe_failed", null, ex.getMessage());
        }
    }

    private ToolOAuthLoginError probeTokenEndpoint(ClientRegistration registration) {
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (!StringUtils.hasText(tokenUri)) {
            return null;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", PROBE_CODE);
        form.add("redirect_uri", registration.getRedirectUri());

        try {
            webClient
                    .post()
                    .uri(tokenUri)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(registration.getClientId(), registration.getClientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return null;
        } catch (WebClientResponseException ex) {
            String body = safeBody(ex);
            String normalized = body.toLowerCase();
            if (normalized.contains("invalid_grant")) {
                return null;
            }
            return errorMapper.mapPreflight(ex);
        } catch (Exception ex) {
            return errorMapper.mapPreflight("token_probe_failed", null, ex.getMessage());
        }
    }

    private static String basicAuth(String clientId, String clientSecret) {
        String value = clientId + ":" + (clientSecret != null ? clientSecret : "");
        return "Basic "
                + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String safeBody(WebClientResponseException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            return body != null ? body : "";
        } catch (Exception ignored) {
            return "";
        }
    }
}
