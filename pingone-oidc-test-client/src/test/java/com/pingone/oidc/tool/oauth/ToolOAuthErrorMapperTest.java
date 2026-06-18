package com.pingone.oidc.tool.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class ToolOAuthErrorMapperTest {

    private final ToolOAuthErrorMapper mapper = new ToolOAuthErrorMapper();

    @Test
    void mapsPingOneNotFoundToActionableMessage() {
        String body =
                """
                {"id":"abc","code":"NOT_FOUND","message":"The request could not be completed.","details":[{"code":"NOT_FOUND","message":"The requested resource was not found."}]}
                """;

        ToolOAuthLoginError error = mapper.mapPreflight("404", body, "404 Not Found");

        assertThat(error.errorCode()).isEqualTo("NOT_FOUND");
        assertThat(error.userMessage()).contains("Client ID and Issuer URI");
        assertThat(error.hints()).isNotEmpty();
    }

    @Test
    void mapsInvalidClientFromOAuth2AuthenticationException() {
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("invalid_client", "Client authentication failed", null));

        ToolOAuthLoginError error = mapper.mapCallback(exception);

        assertThat(error.errorCode()).isEqualTo("invalid_client");
        assertThat(error.userMessage()).contains("Client ID or Client Secret");
        assertThat(error.phase()).isEqualTo("callback");
    }
}
