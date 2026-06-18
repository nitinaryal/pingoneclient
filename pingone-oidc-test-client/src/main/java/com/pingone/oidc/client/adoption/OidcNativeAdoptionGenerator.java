package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import org.springframework.stereotype.Component;

@Component
public class OidcNativeAdoptionGenerator extends OidcPublicClientAdoptionGenerator {

    public OidcNativeAdoptionGenerator(PingOneApplicationTypeCatalog catalog) {
        super(catalog, PingOneApplicationType.OIDC_NATIVE, "oidc-native", true);
    }
}
