package com.pingone.oidc.client.guide;

import com.pingone.oidc.client.PingOneClientIntegration;
import java.util.List;

/**
 * Single navigation index for PingOne client integration code. Referenced from /tool adoption artifacts.
 */
public final class PingOneClientPackageIndex {

    private PingOneClientPackageIndex() {}

    public static String format(PingOneClientIntegration integration) {
        String type = integration.configValue();
        return """
                === PingOne Client Integration Map (%s) ===

                All shareable code lives under com.pingone.oidc.client and com.pingone.oidc.config.

                START HERE
                  1. application.yml tab  — paste OAuth2 + pingone.* settings
                  2. Library tab          — add Maven/Gradle dependency (recommended)
                  3. Copy manifest tab    — copy packages if not using the library
                  4. Java Integration tab — login URLs and wiring notes

                PACKAGE INDEX (library JAR or copy from repo)
                  com.pingone.oidc.client.registration
                    PingOneRegistrationFactory          — contract for ClientRegistration builders
                    OidcWebAppRegistrationFactory       — confidential web app (client secret)
                    OidcPublicClientRegistrationFactory   — SPA (PKCE, no secret)
                    OidcNativeRegistrationFactory       — native app (PKCE, loopback redirect)
                    WorkerRegistrationFactory           — client_credentials
                    RegistrationFactorySupport            — issuer / endpoint helpers

                  com.pingone.oidc.config.properties
                    PingOneClientProperties             — pingone.* configuration
                    PingOneApplicationType              — oidc-web-app | oidc-spa | oidc-native | worker

                  com.pingone.oidc.config.security
                    PingOneSecurityConfigurerFactory      — picks configurer from application-type
                    OidcWebAppSecurityConfigurer          — oauth2Login (web app)
                    OidcSpaSecurityConfigurer             — oauth2Login + PKCE (SPA)
                    OidcNativeSecurityConfigurer          — oauth2Login + PKCE (native)
                    WorkerSecurityConfigurer              — oauth2Client only (machine)

                  com.pingone.oidc.config
                    PingOneOAuthClientManagerConfiguration — OAuth2AuthorizedClientManager (worker tokens)

                RUNTIME ENTRY POINTS
                  Browser login : GET /oauth2/authorization/{registrationId}
                  Browser logout: POST /logout
                  Worker token  : use OAuth2AuthorizedClientManager with registration id

                SETUP STEPS FOR THIS TYPE
                %s"""
                .formatted(type, formatSteps(integration.setupSteps()));
    }

    private static String formatSteps(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            builder.append("  ").append(i + 1).append(". ").append(steps.get(i)).append('\n');
        }
        return builder.toString().stripTrailing();
    }
}
