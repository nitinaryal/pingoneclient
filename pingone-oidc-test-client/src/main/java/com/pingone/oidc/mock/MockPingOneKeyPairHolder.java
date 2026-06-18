package com.pingone.oidc.mock;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneKeyPairHolder {

    private static final String KEY_ID = "mock-pingone-key";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return KEY_ID;
    }

    public Map<String, Object> toJwksDocument() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return Map.of("keys", List.of(rsaKey.toJSONObject()));
    }
}
