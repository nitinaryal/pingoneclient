package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ToolOAuthErrorMapper {

    public ToolOAuthLoginError mapPreflight(WebClientResponseException ex) {
        String body = safeBody(ex);
        return map("preflight", String.valueOf(ex.getStatusCode().value()), body, ex.getMessage());
    }

    public ToolOAuthLoginError mapPreflight(String errorCode, String body, String technicalDetail) {
        return map("preflight", errorCode, body, technicalDetail);
    }

    public ToolOAuthLoginError mapCallback(Throwable throwable) {
        OAuth2Error oauth2Error = resolveOAuth2Error(throwable);
        if (oauth2Error != null) {
            return map("callback", oauth2Error.getErrorCode(), oauth2Error.getDescription(), throwable.getMessage());
        }
        return map("callback", "authentication_failed", null, throwable != null ? throwable.getMessage() : null);
    }

    public ToolOAuthLoginError mapAuthorizationRedirect(String error, String errorDescription) {
        return map("authorization", error, errorDescription, errorDescription);
    }

    private ToolOAuthLoginError map(String phase, String errorCode, String body, String technicalDetail) {
        String normalizedCode = normalizeCode(errorCode, body, technicalDetail);
        String userMessage = userMessage(normalizedCode, body, technicalDetail);
        List<String> hints = hints(normalizedCode, phase);
        return ToolOAuthLoginError.of(
                phase,
                normalizedCode,
                userMessage,
                combineTechnical(body, technicalDetail),
                hints);
    }

    private static OAuth2Error resolveOAuth2Error(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OAuth2AuthenticationException oauth2AuthenticationException) {
                return oauth2AuthenticationException.getError();
            }
            if (current instanceof OAuth2AuthorizationException oauth2AuthorizationException) {
                return oauth2AuthorizationException.getError();
            }
            current = current.getCause();
        }
        return null;
    }

    private static String normalizeCode(String errorCode, String body, String technicalDetail) {
        String combined = (StringUtils.hasText(body) ? body : "")
                + " "
                + (StringUtils.hasText(technicalDetail) ? technicalDetail : "")
                + " "
                + (StringUtils.hasText(errorCode) ? errorCode : "");
        String lower = combined.toLowerCase(Locale.ROOT);
        if (lower.contains("not_found") || lower.contains("requested resource was not found")) {
            return "NOT_FOUND";
        }
        if (lower.contains("invalid_client")) {
            return "invalid_client";
        }
        if (lower.contains("invalid_grant")) {
            return "invalid_grant";
        }
        if (lower.contains("unauthorized_client")) {
            return "unauthorized_client";
        }
        if (lower.contains("access_denied")) {
            return "access_denied";
        }
        if (lower.contains("invalid_redirect_uri") || lower.contains("redirect_uri")) {
            return "invalid_redirect_uri";
        }
        return StringUtils.hasText(errorCode) ? errorCode : "unknown_error";
    }

    private static String userMessage(String errorCode, String body, String technicalDetail) {
        return switch (errorCode) {
            case "NOT_FOUND" ->
                    "PingOne could not find the OAuth client or environment for the configured Client ID and Issuer URI.";
            case "invalid_client" ->
                    "Client authentication failed. The Client ID or Client Secret is incorrect for this PingOne environment.";
            case "invalid_grant" ->
                    "Token exchange failed. This often indicates a wrong Client Secret, redirect URI mismatch, or an expired authorization code.";
            case "unauthorized_client" ->
                    "This OAuth client is not allowed to use the authorization code flow with the current settings.";
            case "access_denied" -> "PingOne denied the login request or the user cancelled sign-in.";
            case "invalid_redirect_uri" ->
                    "The Redirect URI in the wizard does not match a redirect URI registered for this PingOne application.";
            case "401", "403" ->
                    "PingOne rejected the client credentials (HTTP " + errorCode + "). Verify Client ID and Client Secret.";
            default ->
                    "OAuth login failed before or during sign-in. Review the technical details below and verify wizard settings.";
        };
    }

    private static List<String> hints(String errorCode, String phase) {
        List<String> hints = new ArrayList<>();
        switch (errorCode) {
            case "NOT_FOUND" -> {
                hints.add("Confirm the Issuer URI matches your PingOne environment (for example https://auth.pingone.com/{environment-id}/as).");
                hints.add("Confirm the Client ID exists in that same PingOne environment and application.");
            }
            case "invalid_client" -> {
                hints.add("Copy Client ID and Client Secret exactly from PingOne Admin for the selected application.");
                hints.add("Re-import discovery JSON or use Load Current Runtime Config if you switched environments.");
            }
            case "invalid_grant" -> {
                hints.add("Verify Client Secret and Redirect URI match the PingOne application configuration.");
                hints.add("Ensure the Redirect URI is registered exactly, including scheme, host, port, and path.");
            }
            case "invalid_redirect_uri" ->
                    hints.add("Register the wizard Redirect URI in PingOne Admin under the application's redirect URIs.");
            default -> hints.add("Run Connectivity Checks in section 4 to verify issuer metadata and JWKS.");
        }
        if ("preflight".equals(phase)) {
            hints.add("This check ran before opening the PingOne sign-in page so you can fix configuration without completing a full login.");
        }
        return hints;
    }

    private static String combineTechnical(String body, String technicalDetail) {
        if (StringUtils.hasText(body) && StringUtils.hasText(technicalDetail) && !body.equals(technicalDetail)) {
            return technicalDetail + "\n\n" + body;
        }
        return StringUtils.hasText(body) ? body : technicalDetail;
    }

    private static String safeBody(WebClientResponseException ex) {
        try {
            return ex.getResponseBodyAsString();
        } catch (Exception ignored) {
            return ex.getMessage();
        }
    }
}
