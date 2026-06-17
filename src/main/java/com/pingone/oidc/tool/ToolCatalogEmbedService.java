package com.pingone.oidc.tool;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ToolCatalogEmbedService {

    private final PingOneApplicationTypeCatalog catalog;
    private final JsonMapper jsonMapper;

    public ToolCatalogEmbedService(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
        this.jsonMapper = JsonMapper.builder().build();
    }

    public String catalogJson() {
        return jsonMapper.writeValueAsString(catalog.all());
    }
}
