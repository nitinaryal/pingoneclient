package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.ToolWizardConfigService;
import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.trace.FlowActor;
import com.pingone.oidc.tool.trace.PingOneFlowTraceService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;

@Service
public class ToolOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthService.class);

    private final ClientToolRegistrationFactory registrationFactory;
    private final ToolWizardConfigService wizardConfigService;
    private final ToolWizardSessionStore wizardSessionStore;
    private final ToolOAuthPreflightService preflightService;
    private final ToolOAuthLoginErrorStore loginErrorStore;
    private final PingOneFlowTraceService flowTraceService;

    public ToolOAuthService(
            ClientToolRegistrationFactory registrationFactory,
            ToolWizardConfigService wizardConfigService,
            ToolWizardSessionStore wizardSessionStore,
            ToolOAuthPreflightService preflightService,
            ToolOAuthLoginErrorStore loginErrorStore,
            PingOneFlowTraceService flowTraceService) {
        this.registrationFactory = registrationFactory;
        this.wizardConfigService = wizardConfigService;
        this.wizardSessionStore = wizardSessionStore;
        this.preflightService = preflightService;
        this.loginErrorStore = loginErrorStore;
        this.flowTraceService = flowTraceService;
    }

    public Map<String, Object> validateLogin(ClientToolConfigRequest request) {
        flowTraceService.beginFlow("login");
        flowTraceService.record(
                "login",
                FlowActor.BROWSER,
                FlowActor.TEST_CLIENT,
                "Validate wizard OAuth config",
                "Pre-login validation started (client ID, secret, issuer, redirect URI)",
                "info");
        ClientToolConfigRequest saved = wizardConfigService.persistSession(request, false);
        return toValidationResult(preflightService.validate(saved));
    }

    public Map<String, Object> consumeLastLoginError() {
        return loginErrorStore
                .consume()
                .map(this::toErrorMap)
                .orElseGet(() -> Map.of("present", false));
    }

    public Map<String, Object> prepareLogin(ClientToolConfigRequest request) {
        flowTraceService.ensureFlow("login");
        ClientToolConfigRequest saved = wizardConfigService.persistSession(request, true);
        ClientRegistration registration = registrationFactory.build(saved);
        String loginPath = "/oauth2/authorization/" + registration.getRegistrationId();
        flowTraceService.record(
                "login",
                FlowActor.TEST_CLIENT,
                FlowActor.BROWSER,
                "Prepare OAuth redirect",
                "Login path " + loginPath + " for clientId=" + registration.getClientId(),
                "info");
        log.info(
                "Tool login prepared for registration '{}' (clientId={})",
                registration.getRegistrationId(),
                registration.getClientId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loginPath", loginPath);
        result.put("registrationId", registration.getRegistrationId());
        result.put("clientId", registration.getClientId());
        result.put("redirectUri", registration.getRedirectUri());
        result.put("issuerUri", registration.getProviderDetails().getIssuerUri());
        result.put("usingWizardConfig", true);
        result.put("sessionSaved", true);
        result.put(
                "message",
                "Redirecting to PingOne authorization using wizard Client & Application Settings.");
        return result;
    }

    private Map<String, Object> toValidationResult(ToolOAuthLoginError error) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (error == null) {
            result.put("valid", true);
            result.put(
                    "message",
                    "Client ID, Client Secret, issuer, and redirect URI passed pre-login checks. Opening PingOne sign-in...");
            return result;
        }
        loginErrorStore.save(error);
        result.put("valid", false);
        result.put("error", toErrorMap(error));
        return result;
    }

    private Map<String, Object> toErrorMap(ToolOAuthLoginError error) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("present", true);
        mapped.put("phase", error.phase());
        mapped.put("errorCode", error.errorCode());
        mapped.put("userMessage", error.userMessage());
        mapped.put("technicalDetail", error.technicalDetail());
        mapped.put("hints", error.hints());
        return mapped;
    }

    public Map<String, Object> prepareLogout() {
        flowTraceService.beginFlow("logout");
        flowTraceService.record(
                "logout",
                FlowActor.BROWSER,
                FlowActor.TEST_CLIENT,
                "Prepare logout",
                "Tool logout initiated — local session clear + PingOne end session",
                "info");
        wizardSessionStore.setToolInitiatedLogout(true);
        log.info("Tool logout prepared; user will return to /tool after PingOne end session");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logoutPath", "/logout");
        result.put(
                "message",
                "Local session will be cleared and PingOne end session will run (same flow as dashboard logout).");
        return result;
    }
}
