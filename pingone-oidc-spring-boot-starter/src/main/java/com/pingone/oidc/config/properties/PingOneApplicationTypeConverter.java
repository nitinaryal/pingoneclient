package com.pingone.oidc.config.properties;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class PingOneApplicationTypeConverter implements Converter<String, PingOneApplicationType> {

    @Override
    public PingOneApplicationType convert(String source) {
        return PingOneApplicationType.fromConfigValue(source);
    }
}
