package com.pingone.oidc.client.adoption;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

/** Shared YAML/env helpers for adoption artifact generators. */
public final class SharedAdoptionYamlSupport {

    private SharedAdoptionYamlSupport() {}

    public static String buildProviderYamlBlock(
            String providerId, ClientToolConfigRequest request, String issuerUri) {
        String indent = "                          ";
        StringBuilder block = new StringBuilder();
        block.append(indent).append(providerId).append(":\n");
        block.append(indent).append("  issuer-uri: ").append(issuerUri).append('\n');
        appendProviderYamlLine(block, indent, "authorization-uri", request, "authorizationUri");
        appendProviderYamlLine(block, indent, "token-uri", request, "tokenUri");
        appendProviderYamlLine(block, indent, "user-info-uri", request, "userInfoUri");
        appendProviderYamlLine(block, indent, "jwk-set-uri", request, "jwksUri");
        block.append(indent).append("  user-name-attribute: sub\n");
        return block.toString();
    }

    public static String buildDiscoveryMetadataYamlBlock(ClientToolConfigRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("  oidc:");
        lines.add("    discovery-metadata:");
        appendMetadataYamlLine(lines, request, "responseTypesSupported", "response-types-supported");
        appendMetadataYamlLine(lines, request, "responseModesSupported", "response-modes-supported");
        appendMetadataYamlLine(lines, request, "grantTypesSupported", "grant-types-supported");
        appendMetadataYamlLine(lines, request, "subjectTypesSupported", "subject-types-supported");
        appendMetadataYamlLine(lines, request, "idTokenSigningAlgValuesSupported", "id-token-signing-alg-values-supported");
        appendMetadataYamlLine(
                lines, request, "tokenEndpointAuthMethodsSupported", "token-endpoint-auth-methods-supported");
        appendMetadataYamlLine(lines, request, "codeChallengeMethodsSupported", "code-challenge-methods-supported");
        appendMetadataYamlLine(lines, request, "claimsSupported", "claims-supported");
        appendMetadataYamlLine(lines, request, "claimTypesSupported", "claim-types-supported");
        appendMetadataYamlLine(lines, request, "acrValuesSupported", "acr-values-supported");
        appendMetadataYamlLine(
                lines, request, "requestObjectSigningAlgValuesSupported", "request-object-signing-alg-values-supported");
        appendMetadataYamlLine(lines, request, "serviceDocumentation", "service-documentation");
        appendMetadataYamlLine(lines, request, "endSessionEndpoint", "end-session-endpoint");
        appendMetadataYamlLine(lines, request, "revocationUri", "revocation-endpoint");
        appendMetadataYamlLine(lines, request, "introspectionUri", "introspection-endpoint");
        appendMetadataYamlLine(lines, request, "registrationEndpoint", "registration-endpoint");
        if (lines.size() <= 2) {
            return "";
        }
        return "\n" + String.join("\n", lines);
    }

    public static String buildDiscoveryEnvBlock(ClientToolConfigRequest request) {
        StringBuilder block = new StringBuilder();
        appendEnvLine(block, request, "authorizationUri", "PINGONE_AUTHORIZATION_URI");
        appendEnvLine(block, request, "tokenUri", "PINGONE_TOKEN_URI");
        appendEnvLine(block, request, "userInfoUri", "PINGONE_USERINFO_URI");
        appendEnvLine(block, request, "jwksUri", "PINGONE_JWKS_URI");
        return block.toString().stripTrailing();
    }

    public static String buildDiscoveryJavaNotes(ClientToolConfigRequest request) {
        StringBuilder notes = new StringBuilder();
        appendJavaNoteLine(notes, request, "authorizationUri", "Authorization");
        appendJavaNoteLine(notes, request, "tokenUri", "Token");
        appendJavaNoteLine(notes, request, "userInfoUri", "UserInfo");
        appendJavaNoteLine(notes, request, "jwksUri", "JWKS");
        appendJavaNoteLine(notes, request, "endSessionEndpoint", "End session (PingOne signoff)");
        if (notes.isEmpty()) {
            return "//    (import discovery JSON in /tool to populate endpoint overrides)";
        }
        return notes.toString().stripTrailing();
    }

    public static String formatYamlScopes(String scopes) {
        return scopes.replace(",", " ").trim().lines()
                .flatMap(line -> Arrays.stream(line.split("[,\\s]+")))
                .filter(s -> !s.isBlank())
                .map(s -> "              - " + s)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("              - openid");
    }

    private static void appendProviderYamlLine(
            StringBuilder block, String indent, String yamlKey, ClientToolConfigRequest request, String valueKey) {
        String value = request.value(valueKey, "");
        if (StringUtils.hasText(value)) {
            block.append(indent).append("  ").append(yamlKey).append(": ").append(value).append('\n');
        }
    }

    private static void appendMetadataYamlLine(
            List<String> lines, ClientToolConfigRequest request, String valueKey, String yamlKey) {
        String value = request.value(valueKey, "");
        if (StringUtils.hasText(value)) {
            lines.add("      " + yamlKey + ": \"" + value.replace("\"", "\\\"") + "\"");
        }
    }

    private static void appendEnvLine(StringBuilder block, ClientToolConfigRequest request, String key, String envName) {
        String value = request.value(key, "");
        if (StringUtils.hasText(value)) {
            block.append(envName).append('=').append(value).append('\n');
        }
    }

    private static void appendJavaNoteLine(
            StringBuilder notes, ClientToolConfigRequest request, String key, String label) {
        String value = request.value(key, "");
        if (StringUtils.hasText(value)) {
            notes.append("//    ").append(label).append(": ").append(value).append('\n');
        }
    }
}
