(function () {
    let catalog = [];
    const runtime = {
        loginPath: document.querySelector('meta[name="runtime-login-path"]')?.content || '/oauth2/authorization/pingone'
    };

    let selectedType = catalog.find(t => t.implementedInTemplate) || catalog[0];
    let activeSnippetTab = 'yaml';
    let generated = null;

    const typeSelector = document.getElementById('type-selector');
    const typeSummary = document.getElementById('type-summary');
    const configForm = document.getElementById('config-form');
    const testSuite = document.getElementById('test-suite');
    const snippetContent = document.getElementById('snippet-content');
    const diagnosticsOutput = document.getElementById('diagnostics-output');
    const discoveryJsonInput = document.getElementById('discovery-json-input');
    const discoveryImportStatus = document.getElementById('discovery-import-status');
    const discoveryFileInput = document.getElementById('discovery-file-input');
    const artifactsStatus = document.getElementById('artifacts-status');
    const artifactsSection = document.getElementById('artifacts-section');
    const snippetTabs = document.getElementById('snippet-tabs');

    const SNIPPET_TAB_LABELS = {
        yaml: 'application.yml',
        env: 'Environment Variables',
        java: 'Java Integration',
        admin: 'PingOne Admin Checklist'
    };

    const SNIPPET_PLACEHOLDERS = {
        yaml: 'Spring Boot application.yml will appear here after generation.\n\nTip: Fill Client ID, Client Secret, Issuer URI, and Redirect URI in section 2, then click Generate Adoption Artifacts.',
        env: 'Environment variable export (PINGONE_CLIENT_ID, PINGONE_ISSUER_URI, etc.) will appear here after generation.',
        java: 'Java integration notes (packages to copy, login URL, redirect URIs) will appear here after generation.',
        admin: 'PingOne Admin console checklist (redirect URIs, grant types, scopes) will appear here after generation.'
    };

    const FIELD_GROUPS = [
        { id: 'client', title: 'Client & Application Settings' },
        { id: 'discovery-endpoints', title: 'OIDC Provider Endpoints (override discovery)' },
        { id: 'discovery-metadata', title: 'OIDC Discovery Metadata (reference)' }
    ];

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;');
    }

    function renderTypeSelector() {
        typeSelector.innerHTML = catalog.map(type => `
            <button type="button" class="type-card ${type.configValue === selectedType.configValue ? 'active' : ''}"
                    data-type="${type.configValue}">
                <span class="type-name">${escapeHtml(type.displayName)}</span>
                <span class="type-status ${type.implementedInTemplate ? 'implemented' : 'planned'}">
                    ${type.implementedInTemplate ? 'Runnable' : 'Planned'}
                </span>
            </button>
        `).join('');

        typeSelector.querySelectorAll('.type-card').forEach(btn => {
            btn.addEventListener('click', () => {
                selectedType = catalog.find(t => t.configValue === btn.dataset.type);
                renderAll();
            });
        });
    }

    function renderTypeSummary() {
        typeSummary.innerHTML = `
            <p><strong>${escapeHtml(selectedType.displayName)}</strong> — ${escapeHtml(selectedType.summary)}</p>
        `;
    }

    function fieldHtml(field) {
        const required = field.required ? '<span class="required">*</span>' : '';
        const value = field.defaultValue || '';
        const group = field.group || 'client';
        return `
            <label class="form-field" data-field-group="${escapeHtml(group)}">
                <span class="label-row">
                    ${escapeHtml(field.label)} ${required}
                    <span class="tooltip-icon" title="${escapeHtml(field.tooltip)}">?</span>
                </span>
                <input type="text" name="${escapeHtml(field.key)}" value="${escapeHtml(value)}"
                       placeholder="${escapeHtml(field.placeholder)}"
                       data-required="${field.required}"
                       data-discovery-key="${escapeHtml(field.key)}"/>
                <small class="field-hint">YAML: <code>${escapeHtml(field.yamlHint)}</code>
                ${field.envVar && field.envVar !== '—' ? ` · Env: <code>${escapeHtml(field.envVar)}</code>` : ''}</small>
            </label>
        `;
    }

    function renderConfigForm() {
        const grouped = {};
        selectedType.configFields.forEach(field => {
            const group = field.group || 'client';
            if (!grouped[group]) {
                grouped[group] = [];
            }
            grouped[group].push(field);
        });
        configForm.innerHTML = FIELD_GROUPS
            .filter(group => grouped[group.id]?.length)
            .map(group => `
                <fieldset>
                    <legend>${escapeHtml(group.title)}</legend>
                    <div class="field-grid">
                        ${grouped[group.id].map(fieldHtml).join('')}
                    </div>
                </fieldset>
            `)
            .join('');
    }

    function setDiscoveryImportStatus(message) {
        discoveryImportStatus.textContent = message;
    }

    function applyDiscoveryResult(result, sourceLabel) {
        let applied = 0;
        Object.entries(result.fieldValues || {}).forEach(([key, value]) => {
            const input = configForm.querySelector(`input[name="${key}"]`);
            if (input && value) {
                input.value = value;
                input.classList.add('discovery-filled');
                applied += 1;
            }
        });
        const mapped = (result.mappedKeys || []).join(', ') || 'none';
        const unmapped = (result.unmappedKeys || []).length
            ? result.unmappedKeys.join(', ')
            : 'none';
        setDiscoveryImportStatus(
            `${sourceLabel}: applied ${applied} field(s).\nMapped: ${mapped}\nUnmapped keys: ${unmapped}`
        );
    }

    async function applyDiscoveryJson() {
        const json = discoveryJsonInput.value.trim();
        if (!json) {
            alert('Paste OIDC discovery JSON first.');
            return;
        }
        const response = await fetch('/tool/api/discovery/apply', {
            method: 'POST',
            headers: jsonRequestHeaders(),
            body: json
        });
        if (!response.ok) {
            const message = await response.text();
            setDiscoveryImportStatus('Import failed: ' + message);
            return;
        }
        applyDiscoveryResult(await response.json(), 'JSON import');
    }

    async function fetchDiscoveryFromIssuer() {
        const issuerUri = configForm.querySelector('[name="issuerUri"]')?.value.trim()
            || discoveryJsonInput.value.trim();
        if (!issuerUri) {
            alert('Enter Issuer URI in the form or paste a discovery JSON with an issuer field.');
            return;
        }
        let issuer = issuerUri;
        if (issuerUri.startsWith('{')) {
            try {
                issuer = JSON.parse(issuerUri).issuer;
            } catch (error) {
                alert('Invalid issuer value.');
                return;
            }
        }
        setDiscoveryImportStatus('Fetching discovery from ' + issuer + ' ...');
        const response = await fetch('/tool/api/discovery/fetch?issuerUri=' + encodeURIComponent(issuer));
        if (!response.ok) {
            const message = await response.text();
            setDiscoveryImportStatus('Fetch failed: ' + message);
            return;
        }
        const result = await response.json();
        if (result.rawDocument) {
            discoveryJsonInput.value = JSON.stringify(result.rawDocument, null, 2);
        }
        applyDiscoveryResult(result, 'Issuer fetch');
    }

    function clearDiscoveryImport() {
        discoveryJsonInput.value = '';
        configForm.querySelectorAll('input.discovery-filled').forEach(input => {
            input.classList.remove('discovery-filled');
        });
        setDiscoveryImportStatus('Discovery import cleared. Form values were not reset.');
    }

    function bindDiscoveryImport() {
        document.getElementById('btn-discovery-apply').addEventListener('click', applyDiscoveryJson);
        document.getElementById('btn-discovery-fetch').addEventListener('click', fetchDiscoveryFromIssuer);
        document.getElementById('btn-discovery-clear').addEventListener('click', clearDiscoveryImport);
        document.getElementById('btn-discovery-upload').addEventListener('click', () => discoveryFileInput.click());
        discoveryFileInput.addEventListener('change', async () => {
            const file = discoveryFileInput.files?.[0];
            if (!file) {
                return;
            }
            discoveryJsonInput.value = await file.text();
            discoveryFileInput.value = '';
            setDiscoveryImportStatus('Loaded file: ' + file.name + '. Click "Apply JSON to Form".');
        });
        configForm.addEventListener('input', event => {
            if (event.target.matches('input[data-discovery-key]')) {
                event.target.classList.remove('discovery-filled');
            }
        });
    }

    function collectFormValues() {
        const values = {};
        configForm.querySelectorAll('input[name]').forEach(input => {
            values[input.name] = input.value.trim();
        });
        return values;
    }

    function validateForm() {
        const missing = [];
        configForm.querySelectorAll('input[data-required="true"]').forEach(input => {
            if (!input.value.trim()) {
                missing.push(input.name);
            }
        });
        if (missing.length) {
            alert('Please fill required fields: ' + missing.join(', '));
            return false;
        }
        return true;
    }

    function setArtifactsStatus(message, tone) {
        artifactsStatus.textContent = message;
        artifactsStatus.className = 'artifacts-status' + (tone ? ' ' + tone : '');
    }

    function switchSnippetTab(tabId) {
        activeSnippetTab = tabId;
        snippetTabs.querySelectorAll('.snippet-tab').forEach(tab => {
            tab.classList.toggle('active', tab.dataset.tab === tabId);
        });
        renderSnippet();
    }

    function renderSnippet() {
        if (!generated) {
            snippetContent.textContent = SNIPPET_PLACEHOLDERS[activeSnippetTab]
                || SNIPPET_PLACEHOLDERS.yaml;
            return;
        }
        const map = {
            yaml: generated.applicationYaml,
            env: generated.envVariables,
            java: generated.javaIntegrationNotes,
            admin: generated.pingOneAdminSteps
        };
        const content = map[activeSnippetTab];
        snippetContent.textContent = content && content.trim()
            ? content
            : '(No content returned for ' + (SNIPPET_TAB_LABELS[activeSnippetTab] || activeSnippetTab) + ')';
    }

    function renderTestSuite() {
        const regId = configForm.querySelector('[name="registrationId"]')?.value || 'pingone';
        testSuite.innerHTML = selectedType.testFlows.map(test => {
            const path = test.actionPath.replace('{registrationId}', regId);
            const runnableBadge = test.runnableInTemplate
                ? '<span class="badge pass">Runnable in template</span>'
                : '<span class="badge planned">Adoption preview only</span>';
            const steps = test.steps.map(step => `
                <li>
                    <strong>${escapeHtml(step.title)}</strong>
                    <p>${escapeHtml(step.detail)}</p>
                    <p class="expected"><span>Expected:</span> ${escapeHtml(step.expectedResult)}</p>
                </li>
            `).join('');

            let action = '';
            if (test.runnableInTemplate) {
                if (test.id === 'login') {
                    action = `<a class="btn btn-sm btn-primary" href="${escapeHtml(path)}">Run Login</a>`;
                } else if (test.id === 'logout') {
                    const csrfParam = document.querySelector('meta[name=_csrf_parameter]')?.content || '_csrf';
                    const csrfToken = document.querySelector('meta[name=_csrf]')?.content || '';
                    action = `<form action="/logout" method="post" class="inline-form">
                        <input type="hidden" name="${escapeHtml(csrfParam)}" value="${escapeHtml(csrfToken)}"/>
                        <button type="submit" class="btn btn-sm btn-secondary">Run Logout</button>
                    </form>`;
                } else if (test.id === 'connectivity') {
                    action = `<button type="button" class="btn btn-sm btn-primary run-connectivity">Run Check</button>`;
                } else if (test.httpMethod === 'GET') {
                    action = `<a class="btn btn-sm btn-primary" href="${escapeHtml(path)}">Run Test</a>`;
                }
            }
            action += ` <a class="btn btn-sm" href="/tool/test/${escapeHtml(test.id)}">View Steps</a>`;

            return `
                <details class="test-card" ${test.runnableInTemplate ? 'open' : ''}>
                    <summary>
                        <span>${escapeHtml(test.name)}</span>
                        ${runnableBadge}
                    </summary>
                    <p>${escapeHtml(test.description)}</p>
                    <p><code>${escapeHtml(test.httpMethod)} ${escapeHtml(path)}</code></p>
                    <ol class="step-list">${steps}</ol>
                    <div class="actions">${action}</div>
                </details>
            `;
        }).join('');

        testSuite.querySelectorAll('.run-connectivity').forEach(btn => {
            btn.addEventListener('click', runDiagnostics);
        });
    }

    function jsonRequestHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        if (csrfToken) {
            headers['X-CSRF-TOKEN'] = csrfToken;
        }
        return headers;
    }

    async function generateArtifacts(options) {
        const opts = options || {};
        if (!validateForm()) {
            setArtifactsStatus('Generation blocked: fill all required fields in section 2 (marked with *).', 'error');
            return false;
        }
        setArtifactsStatus('Generating artifacts...', 'pending');
        const response = await fetch('/tool/api/generate', {
            method: 'POST',
            headers: jsonRequestHeaders(),
            body: JSON.stringify({
                applicationType: selectedType.configValue,
                values: collectFormValues()
            })
        });
        if (!response.ok) {
            const message = await response.text();
            snippetContent.textContent = 'Generate failed (' + response.status + ').\n\n' + message;
            setArtifactsStatus('Generation failed (' + response.status + '). See panel below.', 'error');
            return false;
        }
        generated = await response.json();
        renderSnippet();
        setArtifactsStatus(
            'Artifacts generated. Use the tabs to view application.yml, Environment Variables, Java Integration, and PingOne Admin Checklist. Click Copy to copy the active tab.',
            'success'
        );
        if (!opts.silentScroll) {
            artifactsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        return true;
    }

    async function onSnippetTabClick(tab) {
        const tabId = tab.dataset.tab;
        switchSnippetTab(tabId);
        if (!generated) {
            setArtifactsStatus(
                'Showing preview for "' + (SNIPPET_TAB_LABELS[tabId] || tabId) + '". Generating artifacts from your form...',
                'pending'
            );
            await generateArtifacts({ silentScroll: true });
        }
    }

    async function runDiagnostics() {
        diagnosticsOutput.textContent = 'Running checks...';
        const response = await fetch('/tool/api/diagnostics');
        const data = await response.json();
        diagnosticsOutput.textContent = JSON.stringify(data, null, 2);
        if (location.hash !== '#diagnostics') {
            location.hash = 'diagnostics';
        }
    }

    async function loadRuntimeDiagnostics() {
        const response = await fetch('/tool/api/diagnostics');
        const data = await response.json();
        const values = {
            registrationId: data.registrationId || 'pingone',
            providerId: data.providerId || 'pingone',
            clientId: '',
            clientSecret: '',
            issuerUri: data.issuerUri || '',
            authorizationUri: data.authorizationUri || '',
            tokenUri: data.tokenUri || '',
            userInfoUri: data.userInfoUri || '',
            jwksUri: data.jwksUri || '',
            redirectUri: data.redirectUri || '',
            postLogoutRedirectUri: data.postLogoutRedirectUri || '',
            scopes: 'openid,profile,email'
        };
        configForm.querySelectorAll('input[name]').forEach(input => {
            if (values[input.name] !== undefined) {
                input.value = values[input.name];
            }
        });
    }

    function bindTabs() {
        snippetTabs.addEventListener('click', event => {
            const tab = event.target.closest('.snippet-tab');
            if (!tab) {
                return;
            }
            onSnippetTabClick(tab);
        });
    }

    function bindCopy() {
        document.querySelectorAll('.copy-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const text = snippetContent.textContent || '';
                await navigator.clipboard.writeText(text);
                btn.textContent = 'Copied!';
                setTimeout(() => { btn.textContent = 'Copy'; }, 1500);
            });
        });
    }

    function renderAll() {
        if (!selectedType) {
            showCatalogError('No application type selected.');
            return;
        }
        renderTypeSelector();
        renderTypeSummary();
        renderConfigForm();
        renderTestSuite();
        renderSnippet();
    }

    function showCatalogLoading() {
        typeSelector.innerHTML = '<p class="notice">Loading application types...</p>';
        typeSummary.innerHTML = '';
        configForm.innerHTML = '';
        testSuite.innerHTML = '';
    }

    function showCatalogError(message) {
        typeSelector.innerHTML = '<p class="notice">Failed to load application types. ' + escapeHtml(message) + '</p>';
    }

    function readEmbeddedCatalog() {
        const element = document.getElementById('tool-catalog-json');
        if (!element?.textContent?.trim()) {
            return null;
        }
        try {
            return JSON.parse(element.textContent);
        } catch (error) {
            console.error('Embedded catalog JSON is invalid', error);
            return null;
        }
    }

    async function fetchCatalog() {
        const response = await fetch('/tool/api/catalog', { cache: 'no-store' });
        if (!response.ok) {
            throw new Error('catalog request failed with status ' + response.status);
        }
        const data = await response.json();
        if (!data.length) {
            throw new Error('catalog is empty');
        }
        return data;
    }

    function applyCatalog(data) {
        catalog = data;
        const previousType = selectedType?.configValue;
        selectedType = catalog.find(t => t.configValue === previousType)
            || catalog.find(t => t.implementedInTemplate)
            || catalog[0];
        renderAll();
    }

    let bindingsReady = false;

    function ensureBindings() {
        if (bindingsReady) {
            return;
        }
        bindingsReady = true;
        bindTabs();
        bindCopy();
        bindDiscoveryImport();
        document.getElementById('btn-generate').addEventListener('click', () => generateArtifacts());
        document.getElementById('btn-diagnostics').addEventListener('click', runDiagnostics);
        document.getElementById('btn-load-runtime').addEventListener('click', loadRuntimeDiagnostics);
    }

    async function init(options) {
        const opts = options || {};
        ensureBindings();
        if (!opts.silent) {
            showCatalogLoading();
        }
        try {
            let data = readEmbeddedCatalog();
            if (!data?.length) {
                data = await fetchCatalog();
            }
            applyCatalog(data);
            if (location.hash === '#diagnostics') {
                runDiagnostics();
            }
        } catch (error) {
            showCatalogError('Refresh the page or sign in and try again.');
            diagnosticsOutput.textContent = String(error);
            console.error('Client tool init failed', error);
        }
    }

    init();

    window.addEventListener('pageshow', event => {
        if (event.persisted) {
            init({ silent: false });
        }
    });
})();
