package com.pingone.oidc.client.registration;

/**
 * Key-value source for building a {@link org.springframework.security.oauth2.client.registration.ClientRegistration}.
 * Implemented by the /tool wizard ({@code ClientToolConfigRequest}) and usable in consumer apps.
 */
public interface RegistrationValueSource {

    String value(String key, String defaultValue);
}
