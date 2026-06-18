package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OidcSpaAdoptionGenerator extends OidcPublicClientAdoptionGenerator {

    public OidcSpaAdoptionGenerator(PingOneApplicationTypeCatalog catalog) {
        super(catalog, PingOneApplicationType.OIDC_SPA, "oidc-spa", true);
    }
}
