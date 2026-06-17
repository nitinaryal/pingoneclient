package com.pingone.oidc.tool.session;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StringUtils;

@Configuration
public class ToolSessionEncryptionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ToolSessionEncryptionConfiguration.class);
    private static final String SALT = "0123456789abcdef";

    @Bean
    TextEncryptor toolWizardSessionEncryptor(
            @Value("${pingone.tool.session.encryption-password:}") String configuredPassword) {
        String password = StringUtils.hasText(configuredPassword)
                ? configuredPassword
                : generateDevelopmentPassword();
        return Encryptors.delux(password, SALT);
    }

    private static String generateDevelopmentPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String generated = Base64.getEncoder().encodeToString(bytes);
        log.warn(
                "pingone.tool.session.encryption-password is not set. "
                        + "Using an ephemeral in-memory key for this JVM lifecycle. "
                        + "Set PINGONE_TOOL_SESSION_ENCRYPTION_PASSWORD for stable encrypted wizard sessions.");
        return generated;
    }
}
