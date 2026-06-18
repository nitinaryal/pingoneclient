package com.pingone.oidc.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.DiscoveryApplyResult;
import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToolWizardConfigServiceTest {

    @Mock
    private OidcDiscoveryImportService discoveryImportService;

    @Mock
    private ToolRuntimeDefaultsService runtimeDefaultsService;

    @Mock
    private ToolWizardSessionStore wizardSessionStore;

    private ToolWizardConfigService service;

    @BeforeEach
    void setUp() {
        PingOneClientProperties properties = new PingOneClientProperties();
        properties.setApplicationType(PingOneApplicationType.OIDC_WEB_APP);
        service = new ToolWizardConfigService(
                discoveryImportService, runtimeDefaultsService, wizardSessionStore, properties);
    }

    @Test
    void applyDiscoveryMergesMissingFieldsFromRuntimeDefaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("clientId", "from-yml");
        defaults.put("clientSecret", "from-yml-secret");
        defaults.put("issuerUri", "https://auth.example.com/default/as");
        when(runtimeDefaultsService.wizardDefaults()).thenReturn(defaults);

        Map<String, String> discoveryFields = new LinkedHashMap<>();
        discoveryFields.put("issuerUri", "https://auth.pingone.com/env/as");
        discoveryFields.put("tokenUri", "https://auth.pingone.com/env/as/token");
        DiscoveryApplyResult parsed = new DiscoveryApplyResult(
                discoveryFields, Map.of("issuer", "https://auth.pingone.com/env/as"), List.of("issuerUri"), List.of());
        when(discoveryImportService.applyJson(any())).thenReturn(parsed);

        DiscoveryApplyResult result = service.applyDiscoveryJson("{}", "oidc-web-app");

        assertThat(result.sessionSaved()).isTrue();
        assertThat(result.fieldValues())
                .containsEntry("issuerUri", "https://auth.pingone.com/env/as")
                .containsEntry("tokenUri", "https://auth.pingone.com/env/as/token")
                .containsEntry("clientId", "from-yml")
                .containsEntry("clientSecret", "from-yml-secret");
        assertThat(result.defaultsAppliedKeys()).contains("clientId", "clientSecret");

        ArgumentCaptor<ClientToolConfigRequest> captor = ArgumentCaptor.forClass(ClientToolConfigRequest.class);
        verify(wizardSessionStore).save(captor.capture());
        assertThat(captor.getValue().getValues()).containsEntry("clientId", "from-yml");
    }
}
