package com.pingone.oidc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast with a clear message when running against real PingOne without required OAuth2 settings.
 */
@Component
@ConditionalOnProperty(name = "mock", havingValue = "false", matchIfMissing = true)
public class PingOneRealModeStartupValidator {

    private final Environment environment;

    public PingOneRealModeStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void validateOAuthConfiguration() {
        String providerId = environment.getProperty("pingone.provider-id", "pingone");
        String issuerKey = "spring.security.oauth2.client.provider." + providerId + ".issuer-uri";
        String issuerUri = environment.getProperty(issuerKey, "");
        boolean hasIssuer = StringUtils.hasText(issuerUri);
        boolean hasEndpoints = hasText(providerId, "authorization-uri")
                && hasText(providerId, "token-uri")
                && hasText(providerId, "user-info-uri")
                && hasText(providerId, "jwk-set-uri");

        if (hasIssuer || hasEndpoints) {
            if (hasIssuer && issuerUri.contains("{")) {
                throw new IllegalStateException(
                        "Invalid PingOne issuer URI '" + issuerUri + "'. "
                                + "Replace placeholders like {environment-id} with your PingOne environment UUID, "
                                + "or run with mock mode: mvn spring-boot:run -pl pingone-oidc-test-client -am -Dmock=true");
            }
            return;
        }

        throw new IllegalStateException(
                "PingOne OAuth2 is not configured. Set PINGONE_ISSUER_URI (and client credentials), "
                        + "or run locally with mock PingOne: .\\run.ps1  (equivalent to -Dmock=true)");
    }

    private boolean hasText(String providerId, String suffix) {
        return StringUtils.hasText(environment.getProperty(
                "spring.security.oauth2.client.provider." + providerId + "." + suffix, ""));
    }
}
