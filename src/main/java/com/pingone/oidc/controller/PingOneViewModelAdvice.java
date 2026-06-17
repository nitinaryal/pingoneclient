package com.pingone.oidc.controller;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PingOneViewModelAdvice {

    private final PingOneClientProperties properties;
    private final boolean mockMode;

    public PingOneViewModelAdvice(
            PingOneClientProperties properties, @Value("${mock:false}") String mockProperty) {
        this.properties = properties;
        this.mockMode = Boolean.parseBoolean(mockProperty);
    }

    @ModelAttribute("pingoneRegistrationId")
    public String pingoneRegistrationId() {
        return properties.getRegistrationId();
    }

    @ModelAttribute("pingoneLoginPath")
    public String pingoneLoginPath() {
        return properties.getLoginPath();
    }

    @ModelAttribute("pingoneApplicationType")
    public String pingoneApplicationType() {
        return properties.getApplicationType().name();
    }

    @ModelAttribute("pingoneMockMode")
    public boolean pingoneMockMode() {
        return mockMode;
    }
}
