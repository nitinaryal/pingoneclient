package com.pingone.oidc.controller;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.ClientAdoptionSnippetService;
import com.pingone.oidc.tool.ClientToolDiagnosticsService;
import com.pingone.oidc.tool.OidcDiscoveryImportService;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.ToolCatalogEmbedService;
import com.pingone.oidc.tool.model.ApplicationTypeDefinition;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.DiscoveryApplyResult;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import com.pingone.oidc.tool.model.TestFlowDefinition;
import com.pingone.oidc.tool.oauth.ToolOAuthService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ClientToolController {

    private final PingOneApplicationTypeCatalog catalog;
    private final ClientAdoptionSnippetService snippetService;
    private final ClientToolDiagnosticsService diagnosticsService;
    private final OidcDiscoveryImportService discoveryImportService;
    private final PingOneClientProperties runtimeProperties;
    private final ToolCatalogEmbedService catalogEmbedService;
    private final ToolOAuthService toolOAuthService;

    public ClientToolController(
            PingOneApplicationTypeCatalog catalog,
            ClientAdoptionSnippetService snippetService,
            ClientToolDiagnosticsService diagnosticsService,
            OidcDiscoveryImportService discoveryImportService,
            PingOneClientProperties runtimeProperties,
            ToolCatalogEmbedService catalogEmbedService,
            ToolOAuthService toolOAuthService) {
        this.catalog = catalog;
        this.snippetService = snippetService;
        this.diagnosticsService = diagnosticsService;
        this.discoveryImportService = discoveryImportService;
        this.runtimeProperties = runtimeProperties;
        this.catalogEmbedService = catalogEmbedService;
        this.toolOAuthService = toolOAuthService;
    }

    @GetMapping("/tool")
    public String tool(Model model) {
        model.addAttribute("runtimeType", runtimeProperties.getApplicationType().getConfigValue());
        model.addAttribute("runtimeRegistrationId", runtimeProperties.getRegistrationId());
        model.addAttribute("runtimeLoginPath", runtimeProperties.getLoginPath());
        model.addAttribute("catalogJson", catalogEmbedService.catalogJson());
        return "tool/index";
    }

    @GetMapping("/tool/test/{testId}")
    public String testGuide(@PathVariable String testId, Model model) {
        ApplicationTypeDefinition typeDef = catalog.find(runtimeProperties.getApplicationType().getConfigValue());
        TestFlowDefinition flow = typeDef.testFlows().stream()
                .filter(test -> test.id().equals(testId))
                .findFirst()
                .orElse(null);
        model.addAttribute("testFlow", flow);
        model.addAttribute("applicationType", typeDef);
        model.addAttribute("runtimeLoginPath", runtimeProperties.getLoginPath());
        return "tool/test-guide";
    }

    @GetMapping(value = "/tool/api/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object catalog() {
        return catalog.all();
    }

    @GetMapping(value = "/tool/api/diagnostics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> diagnostics() {
        return diagnosticsService.runtimeDiagnostics();
    }

    @PostMapping(value = "/tool/api/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public GeneratedAdoptionArtifacts generate(@RequestBody ClientToolConfigRequest request) {
        return snippetService.generate(request);
    }

    @PostMapping(value = "/tool/api/discovery/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DiscoveryApplyResult applyDiscovery(@RequestBody String discoveryJson) {
        try {
            return discoveryImportService.applyJson(discoveryJson);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(value = "/tool/api/oauth/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> prepareOAuthLogin(@RequestBody ClientToolConfigRequest request) {
        try {
            return toolOAuthService.prepareLogin(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping(value = "/tool/api/discovery/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DiscoveryApplyResult fetchDiscovery(
            @RequestParam String issuerUri,
            @RequestParam(required = false) String discoveryPath) {
        try {
            return discoveryImportService.fetchFromIssuer(issuerUri, discoveryPath);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to fetch discovery document: " + ex.getMessage(), ex);
        }
    }
}
