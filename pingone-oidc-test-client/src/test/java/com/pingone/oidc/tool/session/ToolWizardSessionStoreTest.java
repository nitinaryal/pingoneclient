package com.pingone.oidc.tool.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ToolWizardSessionStoreTest {

    private ToolWizardSessionStore store;

    @BeforeEach
    void setUp() {
        store = new ToolWizardSessionStore(Encryptors.delux("test-password", "0123456789abcdef"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void encryptsAndRestoresWizardConfig() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setApplicationType("oidc-web-app");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("clientId", "wizard-client");
        values.put("clientSecret", "wizard-secret");
        values.put("issuerUri", "https://auth.example.com/as");
        request.setValues(values);

        store.save(request, true);

        ClientToolConfigRequest restored = store.load().orElseThrow();
        assertThat(restored.getApplicationType()).isEqualTo("oidc-web-app");
        assertThat(restored.getValues()).containsEntry("clientId", "wizard-client");
        assertThat(restored.getValues()).containsEntry("clientSecret", "wizard-secret");

        HttpServletRequest httpRequest =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Object encrypted = httpRequest.getSession().getAttribute(ToolWizardSessionStore.ENCRYPTED_CONFIG_ATTRIBUTE);
        assertThat(encrypted).isInstanceOf(String.class);
        assertThat((String) encrypted).doesNotContain("wizard-secret");
    }
}
