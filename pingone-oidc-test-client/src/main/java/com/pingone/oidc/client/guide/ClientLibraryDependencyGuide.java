package com.pingone.oidc.client.guide;

import com.pingone.oidc.client.PingOneClientIntegration;

/**
 * Maven / Gradle instructions for consuming PingOne client integration as a library dependency.
 */
public final class ClientLibraryDependencyGuide {

    public static final String GROUP_ID = "com.pingone";
    public static final String ARTIFACT_ID = "pingone-oidc-spring-boot-starter";
    public static final String VERSION_PLACEHOLDER = "1.0.0-SNAPSHOT";

    private ClientLibraryDependencyGuide() {}

    public static String format(PingOneClientIntegration integration) {
        String type = integration.configValue();
        return """
                === Use as Maven / Gradle library (recommended) ===

                The PingOne Spring Boot starter packages registration factories, security configururers,
                and auto-configuration. You only add application.yml (from the YAML tab) and your own UI.

                PREREQUISITE — publish or install locally from this repo:
                  mvn clean install

                MAVEN (pom.xml)
                <dependency>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </dependency>

                Also ensure (usually already present):
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-security</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-oauth2-client</artifactId>
                </dependency>

                GRADLE (build.gradle.kts)
                implementation("%s:%s:%s")
                implementation("org.springframework.boot:spring-boot-starter-security")
                implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

                APPLICATION CONFIGURATION
                  pingone.application-type: %s
                  spring.security.oauth2.client.registration.* — see application.yml tab

                AUTO-CONFIGURATION
                  Starter enables PingOne OAuth2 client integration when on the classpath.
                  No @Import required if using Spring Boot 3+ auto-configuration.

                SECURITY FILTER CHAIN
                  Your app must expose a SecurityFilterChain that calls:
                    pingOneSecurityConfigurerFactory.resolve().configure(http, properties, clientRegistrationRepository);
                  Or copy PingOneSecurityConfig from the test-client template as a starting point.

                ALTERNATIVE — COPY SOURCE
                  If you cannot use the library, use the Copy manifest tab and copy packages listed there.

                REPOSITORY (if not in Maven Central)
                  mvn install:install-file or publish to your internal artifact repository after mvn install.
                """
                .formatted(
                        GROUP_ID,
                        ARTIFACT_ID,
                        VERSION_PLACEHOLDER,
                        GROUP_ID,
                        ARTIFACT_ID,
                        VERSION_PLACEHOLDER,
                        type);
    }
}
