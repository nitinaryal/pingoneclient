package com.pingone.oidc.mock;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockPingOneStartupLogger.class);

    private final PingOneClientProperties properties;

    public MockPingOneStartupLogger(PingOneClientProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String basePath = properties.getMock().getBasePath();
        log.info("============================================================");
        log.info("PingOne MOCK mode is ENABLED (mock=true)");
        log.info("Local OIDC issuer path: {}", basePath);
        log.info("Mock client ID: {}", properties.getMock().getClientId());
        log.info("Use Login with PingOne — you will be redirected to the mock authorize page.");
        log.info("============================================================");
    }
}
