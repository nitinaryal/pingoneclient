package com.pingone.oidc.config;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.config.security.PingOneSecurityConfigurerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for PingOne OIDC client integration (registration factories + security configururers).
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = "org.springframework.security.oauth2.client.registration.ClientRegistration")
@EnableConfigurationProperties(PingOneClientProperties.class)
@ComponentScan(basePackages = {"com.pingone.oidc.client.registration", "com.pingone.oidc.config.security"})
@Import({PingOneOAuthClientManagerConfiguration.class, PingOneClientAutoConfiguration.class, PingOneSecurityConfigurerFactory.class})
public class PingOneOidcAutoConfiguration {}
