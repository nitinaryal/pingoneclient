package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PingOneSecurityConfigurerFactory {

    private final PingOneClientProperties properties;
    private final Map<PingOneApplicationType, PingOneSecurityConfigurer> configururersByType;

    public PingOneSecurityConfigurerFactory(
            PingOneClientProperties properties, List<PingOneSecurityConfigurer> configururers) {
        this.properties = properties;
        this.configururersByType = configururers.stream()
                .collect(Collectors.toMap(PingOneSecurityConfigurer::supportedType, Function.identity()));
    }

    public PingOneSecurityConfigurer resolve() {
        PingOneApplicationType type = properties.getApplicationType();
        PingOneSecurityConfigurer configurer = configururersByType.get(type);
        if (configurer == null) {
            throw new IllegalStateException(
                    "PingOne application type '" + type + "' is not implemented. "
                            + "Supported types: " + configururersByType.keySet());
        }
        return configurer;
    }
}
