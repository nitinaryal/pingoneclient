package com.pingone.oidc.tool.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DiscoveryApplyResult(
        Map<String, String> fieldValues,
        Map<String, Object> rawDocument,
        List<String> mappedKeys,
        List<String> unmappedKeys,
        List<String> defaultsAppliedKeys,
        boolean sessionSaved) {

    public DiscoveryApplyResult(
            Map<String, String> fieldValues,
            Map<String, Object> rawDocument,
            List<String> mappedKeys,
            List<String> unmappedKeys) {
        this(fieldValues, rawDocument, mappedKeys, unmappedKeys, List.of(), false);
    }

    public static DiscoveryApplyResult empty() {
        return new DiscoveryApplyResult(
                new LinkedHashMap<>(), new LinkedHashMap<>(), List.of(), List.of(), List.of(), false);
    }
}
