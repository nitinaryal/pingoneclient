package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;

/**
 * Produces copy-ready configuration artifacts for a PingOne application type after testing in /tool.
 */
public interface AdoptionArtifactGenerator {

    PingOneApplicationType supportedType();

    GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request);
}
