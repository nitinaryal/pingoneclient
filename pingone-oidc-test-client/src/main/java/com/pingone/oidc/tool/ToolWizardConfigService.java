package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.DiscoveryApplyResult;
import com.pingone.oidc.tool.model.ToolWizardSessionView;
import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ToolWizardConfigService {

    private final OidcDiscoveryImportService discoveryImportService;
    private final ToolRuntimeDefaultsService runtimeDefaultsService;
    private final ToolWizardSessionStore wizardSessionStore;
    private final PingOneClientProperties properties;

    public ToolWizardConfigService(
            OidcDiscoveryImportService discoveryImportService,
            ToolRuntimeDefaultsService runtimeDefaultsService,
            ToolWizardSessionStore wizardSessionStore,
            PingOneClientProperties properties) {
        this.discoveryImportService = discoveryImportService;
        this.runtimeDefaultsService = runtimeDefaultsService;
        this.wizardSessionStore = wizardSessionStore;
        this.properties = properties;
    }

    public DiscoveryApplyResult applyDiscoveryJson(String discoveryJson, String applicationType) {
        return mergePersistAndWrap(discoveryImportService.applyJson(discoveryJson), applicationType);
    }

    public DiscoveryApplyResult fetchDiscovery(String issuerUri, String discoveryPath, String applicationType) {
        return mergePersistAndWrap(
                discoveryImportService.fetchFromIssuer(issuerUri, discoveryPath), applicationType);
    }

    public ToolWizardSessionView saveSession(ClientToolConfigRequest request) {
        return saveSession(request, false);
    }

    public ClientToolConfigRequest persistSession(ClientToolConfigRequest request, boolean toolInitiatedLogin) {
        ClientToolConfigRequest normalized = normalize(request);
        wizardSessionStore.save(normalized, toolInitiatedLogin);
        return normalized;
    }

    public ToolWizardSessionView saveSession(ClientToolConfigRequest request, boolean toolInitiatedLogin) {
        ClientToolConfigRequest normalized = persistSession(request, toolInitiatedLogin);
        return toView(normalized, true);
    }

    public ToolWizardSessionView loadSession() {
        return wizardSessionStore
                .load()
                .map(request -> toView(request, true))
                .orElseGet(() -> new ToolWizardSessionView(false, null, Map.of(), List.of()));
    }

    public Map<String, String> runtimeDefaults() {
        return runtimeDefaultsService.wizardDefaults();
    }

    public ClientToolConfigRequest requireSessionConfig() {
        return wizardSessionStore
                .load()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No wizard configuration in session. Complete section 2 or import discovery JSON first."));
    }

    private DiscoveryApplyResult mergePersistAndWrap(DiscoveryApplyResult parsed, String applicationType) {
        Map<String, String> defaults = runtimeDefaultsService.wizardDefaults();
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        List<String> defaultsApplied = new ArrayList<>();

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String discoveryValue = parsed.fieldValues().get(entry.getKey());
            if (!StringUtils.hasText(discoveryValue)) {
                defaultsApplied.add(entry.getKey());
            }
        }

        parsed.fieldValues().forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                merged.put(key, value.trim());
            }
        });

        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setApplicationType(resolveApplicationType(applicationType));
        request.setValues(merged);
        wizardSessionStore.save(request);

        return new DiscoveryApplyResult(
                merged, parsed.rawDocument(), parsed.mappedKeys(), parsed.unmappedKeys(), defaultsApplied, true);
    }

    private ClientToolConfigRequest normalize(ClientToolConfigRequest request) {
        ClientToolConfigRequest normalized = new ClientToolConfigRequest();
        normalized.setApplicationType(resolveApplicationType(request.getApplicationType()));
        Map<String, String> values = new LinkedHashMap<>(runtimeDefaultsService.wizardDefaults());
        if (request.getValues() != null) {
            request.getValues().forEach((key, value) -> {
                if (StringUtils.hasText(value)) {
                    values.put(key, value.trim());
                }
            });
        }
        normalized.setValues(values);
        return normalized;
    }

    private String resolveApplicationType(String applicationType) {
        return StringUtils.hasText(applicationType)
                ? applicationType.trim()
                : properties.getApplicationType().getConfigValue();
    }

    private ToolWizardSessionView toView(ClientToolConfigRequest request, boolean saved) {
        return new ToolWizardSessionView(
                saved,
                request.getApplicationType(),
                new LinkedHashMap<>(request.getValues()),
                List.of("clientId", "clientSecret"));
    }
}
