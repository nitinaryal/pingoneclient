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
    const toolActionBanner = document.getElementById('tool-action-banner');
    const testSuiteStatus = document.getElementById('test-suite-status');
    const diagnosticsStatus = document.getElementById('diagnostics-status');
    const testsSection = document.getElementById('tests-section');
    const loginErrorPanel = document.getElementById('login-error-panel');
    const loginErrorSummary = document.getElementById('login-error-summary');
    const loginErrorHints = document.getElementById('login-error-hints');
    const loginErrorTechnical = document.getElementById('login-error-technical');

    const SNIPPET_TAB_LABELS = {
        yaml: 'application.yml',
        env: 'Environment Variables',
        java: 'Java Integration',
        manifest: 'Copy Manifest',
        guide: 'Adoption Guide',
        library: 'Maven / Gradle Library',
        admin: 'PingOne Admin Checklist'
    };

    const SNIPPET_PLACEHOLDERS = {
        yaml: 'Spring Boot application.yml will appear here after generation.\n\nTip: Fill required fields in section 2, test login, then review generated artifacts.',
        env: 'Environment variable export (PINGONE_CLIENT_ID, PINGONE_ISSUER_URI, etc.) will appear here after generation.',
        java: 'Java integration notes (login URL, redirect URIs, wiring hints) will appear here after generation.',
        manifest: 'Packages and setup steps to copy into your project (if not using the Maven library).',
        guide: 'Navigation map for all PingOne client packages — start here when adopting.',
        library: 'Maven and Gradle dependency coordinates for pingone-oidc-spring-boot-starter.',
        admin: 'PingOne Admin console checklist (redirect URIs, grant types, scopes) will appear here after generation.'
    };

    const FIELD_GROUPS = [
        { id: 'client', title: 'Client & Application Settings' },
        { id: 'discovery-endpoints', title: 'OIDC Provider Endpoints (override discovery)' },
        { id: 'discovery-metadata', title: 'OIDC Discovery Metadata (reference)' }
    ];

    function logToolAction(action, detail) {
        console.info('[PingOne Client Tool]', action + ':', detail);
    }

    function setStatusElement(element, message, tone) {
        if (!element) {
            return;
        }
        element.textContent = message;
        element.className = element.id === 'artifacts-status'
            ? 'artifacts-status' + (tone ? ' ' + tone : '')
            : 'action-status' + (tone ? ' ' + tone : '');
    }

    function setActionBanner(message, tone) {
        if (!toolActionBanner) {
            return;
        }
        if (!message) {
            toolActionBanner.textContent = '';
            toolActionBanner.className = 'tool-action-banner hidden';
            return;
        }
        toolActionBanner.textContent = message;
        toolActionBanner.className = 'tool-action-banner' + (tone ? ' ' + tone : '');
    }

    function setTestSuiteStatus(message, tone) {
        setStatusElement(testSuiteStatus, message, tone);
    }

    function setDiagnosticsStatus(message, tone) {
        setStatusElement(diagnosticsStatus, message, tone);
    }

    async function refreshAuthStatus() {
        try {
            const response = await fetch('/tool/api/auth/status', { cache: 'no-store' });
            if (!response.ok) {
                return;
            }
            const data = await response.json();
            const messageEl = document.getElementById('auth-status-message');
            const loginBtn = document.getElementById('btn-tool-login');
            const logoutBtn = document.getElementById('btn-tool-logout');
            if (messageEl) {
                messageEl.innerHTML = data.authenticated
                    ? 'Signed in as <strong>' + escapeHtml(data.principalName || 'user') + '</strong>.'
                    : 'You are not signed in.';
            }
            if (loginBtn) {
                loginBtn.hidden = !!data.authenticated;
            }
            if (logoutBtn) {
                logoutBtn.hidden = !data.authenticated;
            }
        } catch (error) {
            console.warn('Failed to refresh authentication status', error);
        }
    }

    function submitLogoutForm() {
        const csrfParam = document.querySelector('meta[name=_csrf_parameter]')?.content || '_csrf';
        const csrfToken = document.querySelector('meta[name=_csrf]')?.content || '';
        const form = document.createElement('form');
        form.method = 'post';
        form.action = '/logout';
        form.className = 'inline-form';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = csrfParam;
        input.value = csrfToken;
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    }

    async function runToolOAuthLogout() {
        const buttons = document.querySelectorAll('#btn-tool-logout, .run-tool-logout');
        setActionBanner('Preparing logout (local session + PingOne end session)...', 'pending');
        setTestSuiteStatus('Preparing logout using the same flow as dashboard logout...', 'pending');
        buttons.forEach(btn => { btn.disabled = true; });
        logToolAction('logout', 'preparing');
        try {
            const saved = await persistWizardConfig();
            if (!saved) {
                throw new Error('Could not save wizard configuration before logout');
            }
            const response = await fetch('/tool/api/oauth/logout/prepare', {
                method: 'POST',
                headers: jsonRequestHeaders()
            });
            if (!response.ok) {
                throw new Error(await response.text());
            }
            const result = await response.json();
            logToolAction('logout', result.message || 'submitting POST /logout');
            setActionBanner(result.message, 'pending');
            setTestSuiteStatus('Redirecting to PingOne end session endpoint...', 'pending');
            submitLogoutForm();
        } catch (error) {
            logToolAction('logout', 'failed: ' + error.message);
            setActionBanner('Logout failed: ' + error.message, 'error');
            setTestSuiteStatus('Logout could not start. See message above.', 'error');
            buttons.forEach(btn => { btn.disabled = false; });
        }
    }

    function clearLoginErrorPanel() {
        if (!loginErrorPanel) {
            return;
        }
        loginErrorPanel.classList.add('hidden');
        if (loginErrorSummary) {
            loginErrorSummary.textContent = '';
        }
        if (loginErrorHints) {
            loginErrorHints.innerHTML = '';
        }
        if (loginErrorTechnical) {
            loginErrorTechnical.textContent = '';
        }
    }

    function showLoginError(error) {
        if (!error || !loginErrorPanel) {
            return;
        }
        const code = error.errorCode ? '[' + error.errorCode + '] ' : '';
        if (loginErrorSummary) {
            loginErrorSummary.textContent = code + (error.userMessage || 'OAuth login failed.');
        }
        if (loginErrorHints) {
            loginErrorHints.innerHTML = (error.hints || [])
                .map(hint => '<li>' + escapeHtml(hint) + '</li>')
                .join('');
        }
        if (loginErrorTechnical) {
            const phase = error.phase ? 'Phase: ' + error.phase + '\n\n' : '';
            loginErrorTechnical.textContent = phase + (error.technicalDetail || 'No additional technical detail.');
        }
        loginErrorPanel.classList.remove('hidden');
        testsSection?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function applyLoginValidationFailure(validation) {
        const error = validation.error || {};
        showLoginError(error);
        setActionBanner(error.userMessage || 'Login validation failed before opening PingOne.', 'error');
        setTestSuiteStatus('Login blocked by pre-login validation. Fix Client ID, Client Secret, issuer, or redirect URI.', 'error');
        logToolAction('login', 'validation failed: ' + (error.errorCode || 'unknown'));
    }

    async function loadLastOAuthLoginError() {
        const response = await fetch('/tool/api/oauth/last-error', { cache: 'no-store' });
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        if (!data.present) {
            return;
        }
        showLoginError(data);
        setActionBanner(data.userMessage || 'OAuth login failed.', 'error');
        setTestSuiteStatus('Login failed during sign-in or token exchange. See Login Error details below.', 'error');
        logToolAction('login', 'failed after redirect: ' + (data.errorCode || 'unknown'));
    }

    async function validateWizardLoginConfig() {
        const response = await fetch('/tool/api/oauth/login/validate', {
            method: 'POST',
            headers: jsonRequestHeaders(),
            body: JSON.stringify({
                applicationType: selectedType.configValue,
                values: collectFormValues()
            })
        });
        if (!response.ok) {
            throw new Error(await response.text());
        }
        return response.json();
    }

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
                persistWizardConfig();
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
        const defaults = (result.defaultsAppliedKeys || []).length
            ? result.defaultsAppliedKeys.join(', ')
            : 'none';
        const sessionNote = result.sessionSaved
            ? '\nConfiguration saved in encrypted server session.'
            : '';
        setDiscoveryImportStatus(
            `${sourceLabel}: applied ${applied} field(s).\nMapped: ${mapped}\nFilled from application.yml defaults: ${defaults}\nUnmapped keys: ${unmapped}${sessionNote}`
        );
        if (result.sessionSaved) {
            setArtifactsStatus('Discovery import merged with application.yml defaults and saved to your session.', 'success');
            setActionBanner('Discovery import saved to encrypted session.', 'success');
        }
    }

    function currentConfigPayload() {
        return {
            applicationType: selectedType?.configValue || 'oidc-web-app',
            values: collectFormValues()
        };
    }

    function applyFormValues(values) {
        if (!values) {
            return;
        }
        configForm.querySelectorAll('input[name]').forEach(input => {
            if (values[input.name] !== undefined) {
                input.value = values[input.name];
            }
        });
    }

    async function persistWizardConfig() {
        const response = await fetch('/tool/api/session/persist', {
            method: 'POST',
            headers: jsonRequestHeaders(),
            body: JSON.stringify(currentConfigPayload())
        });
        if (!response.ok) {
            const message = await response.text();
            console.warn('Failed to persist wizard configuration', message);
            logToolAction('persist', 'failed: ' + message);
            return false;
        }
        logToolAction('persist', 'wizard configuration saved to encrypted session');
        return true;
    }

    async function loadSavedWizardConfig() {
        const response = await fetch('/tool/api/session/config', { cache: 'no-store' });
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        if (!data.saved) {
            return;
        }
        if (data.applicationType) {
            selectedType = catalog.find(t => t.configValue === data.applicationType) || selectedType;
        }
        renderAll();
        applyFormValues(data.values);
        setArtifactsStatus(
            'Restored wizard configuration from encrypted server session. Tests will reuse these values.',
            'success'
        );
    }

    async function applyDiscoveryJson() {
        const json = discoveryJsonInput.value.trim();
        if (!json) {
            alert('Paste OIDC discovery JSON first.');
            return;
        }
        const response = await fetch(
            '/tool/api/discovery/apply?applicationType=' + encodeURIComponent(selectedType.configValue),
            {
            method: 'POST',
            headers: jsonRequestHeaders(),
            body: json
        });
        if (!response.ok) {
            const message = await response.text();
            setDiscoveryImportStatus('Import failed: ' + message);
            setActionBanner('Discovery import failed: ' + message, 'error');
            logToolAction('discovery-import', 'failed: ' + message);
            return;
        }
        const result = await response.json();
        applyDiscoveryResult(result, 'JSON import');
        logToolAction('discovery-import', 'applied ' + Object.keys(result.fieldValues || {}).length + ' field(s)');
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
        setActionBanner('Fetching OIDC discovery document from issuer...', 'pending');
        const response = await fetch(
            '/tool/api/discovery/fetch?issuerUri=' + encodeURIComponent(issuer)
                + '&applicationType=' + encodeURIComponent(selectedType.configValue)
        );
        if (!response.ok) {
            const message = await response.text();
            setDiscoveryImportStatus('Fetch failed: ' + message);
            setActionBanner('Discovery fetch failed: ' + message, 'error');
            logToolAction('discovery-fetch', 'failed: ' + message);
            return;
        }
        const result = await response.json();
        if (result.rawDocument) {
            discoveryJsonInput.value = JSON.stringify(result.rawDocument, null, 2);
        }
        applyDiscoveryResult(result, 'Issuer fetch');
        logToolAction('discovery-fetch', 'completed for ' + issuer);
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
            manifest: generated.copyManifest,
            guide: generated.adoptionGuide,
            library: generated.libraryDependency,
            admin: generated.pingOneAdminSteps
        };
        const content = map[activeSnippetTab];
        snippetContent.textContent = content && content.trim()
            ? content
            : '(No content returned for ' + (SNIPPET_TAB_LABELS[activeSnippetTab] || activeSnippetTab) + ')';
    }

    async function runToolOAuthLogin(event) {
        const btn = event?.currentTarget || document.getElementById('btn-tool-login');
        clearLoginErrorPanel();
        if (!validateForm()) {
            setTestSuiteStatus('Login blocked: fill required fields in section 2 (marked with *).', 'error');
            setActionBanner('Complete required wizard fields before login.', 'error');
            return;
        }
        const originalLabel = btn?.textContent;
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Validating...';
        }
        setActionBanner('Validating Client ID, Client Secret, and PingOne endpoints before sign-in...', 'pending');
        setTestSuiteStatus('Running pre-login validation...', 'pending');
        logToolAction('login', 'validating configuration');
        try {
            const validation = await validateWizardLoginConfig();
            if (!validation.valid) {
                applyLoginValidationFailure(validation);
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = originalLabel;
                }
                return;
            }
            if (btn) {
                btn.textContent = 'Preparing login...';
            }
            setActionBanner(validation.message || 'Preparing PingOne authorization redirect...', 'pending');
            setTestSuiteStatus('Pre-login validation passed. Preparing authorization redirect...', 'success');
            const saved = await persistWizardConfig();
            if (!saved) {
                throw new Error('Could not save wizard configuration to encrypted session');
            }
            const response = await fetch('/tool/api/oauth/login', {
                method: 'POST',
                headers: jsonRequestHeaders(),
                body: JSON.stringify({
                    applicationType: selectedType.configValue,
                    values: collectFormValues()
                })
            });
            if (!response.ok) {
                throw new Error(await response.text());
            }
            const result = await response.json();
            logToolAction('login', result.message || ('redirecting to ' + result.loginPath));
            setActionBanner(result.message || 'Redirecting to PingOne authorization endpoint...', 'pending');
            setTestSuiteStatus('Redirecting to PingOne sign-in page...', 'pending');
            window.location.href = result.loginPath;
        } catch (error) {
            logToolAction('login', 'failed: ' + error.message);
            setActionBanner('Login setup failed: ' + error.message, 'error');
            setTestSuiteStatus('Login could not start. Fix configuration and try again.', 'error');
            if (btn) {
                btn.disabled = false;
                btn.textContent = originalLabel;
            }
        }
    }

    async function generateArtifactsAfterSuccessfulTest(source) {
        const ok = await generateArtifacts({ silentScroll: true, highlightGuide: true });
        if (ok) {
            setActionBanner(
                'Login test succeeded — adoption artifacts were generated from your tested configuration. Review section 3 (start with Adoption Guide or Maven / Gradle Library).',
                'success'
            );
            logToolAction('generate', 'auto after ' + source);
            artifactsSection?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            if (source === 'login') {
                switchSnippetTab('guide');
            }
        }
    }

    async function showOAuthReturnMessage() {
        const params = new URLSearchParams(window.location.search);
        let changed = false;
        if (params.get('oauth') === 'success') {
            clearLoginErrorPanel();
            setActionBanner('OAuth login completed successfully using your wizard configuration.', 'success');
            setTestSuiteStatus('Login succeeded. You are signed in and can run logout or OIDC validation tests.', 'success');
            logToolAction('login', 'completed successfully');
            refreshAuthStatus();
            await generateArtifactsAfterSuccessfulTest('login');
            if (location.hash !== '#tests') {
                location.hash = 'tests';
            }
            params.delete('oauth');
            changed = true;
        }
        if (params.get('oauth') === 'error') {
            await loadLastOAuthLoginError();
            testsSection?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            if (location.hash !== '#tests') {
                location.hash = 'tests';
            }
            params.delete('oauth');
            changed = true;
        }
        if (params.get('logout') === 'success') {
            setActionBanner('Logout completed. Local session cleared and PingOne end session finished.', 'success');
            setTestSuiteStatus('Logout succeeded (same flow as dashboard). You are signed out.', 'success');
            logToolAction('logout', 'completed successfully');
            refreshAuthStatus();
            testsSection?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            if (location.hash !== '#tests') {
                location.hash = 'tests';
            }
            params.delete('logout');
            changed = true;
        }
        if (changed) {
            const query = params.toString();
            const nextUrl = window.location.pathname + (query ? '?' + query : '') + window.location.hash;
            window.history.replaceState({}, '', nextUrl);
        }
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
                    action = `<button type="button" class="btn btn-sm btn-primary run-tool-login">Login with PingOne (wizard config)</button>`;
                } else if (test.id === 'logout') {
                    action = `<button type="button" class="btn btn-sm btn-secondary run-tool-logout">Logout (PingOne end session)</button>`;
                } else if (test.id === 'connectivity') {
                    action = `<button type="button" class="btn btn-sm btn-primary run-connectivity">Run Check</button>`;
                } else if (test.httpMethod === 'GET') {
                    action = `<a class="btn btn-sm btn-primary run-get-test" href="${escapeHtml(path)}" target="_blank" rel="noopener noreferrer" data-test-name="${escapeHtml(test.name)}">Run Test</a>`;
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
        testSuite.querySelectorAll('.run-tool-login').forEach(btn => {
            btn.addEventListener('click', runToolOAuthLogin);
        });
        testSuite.querySelectorAll('.run-tool-logout').forEach(btn => {
            btn.addEventListener('click', runToolOAuthLogout);
        });
        testSuite.querySelectorAll('.run-get-test').forEach(link => {
            link.addEventListener('click', () => {
                const testName = link.dataset.testName || link.getAttribute('href');
                setTestSuiteStatus('Opened "' + testName + '" in a new browser tab.', 'info');
                setActionBanner('OIDC validation test opened in a new tab. Return here to continue the test suite.', 'info');
                logToolAction('test', 'opened ' + testName + ' in new tab');
            });
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
        await persistWizardConfig();
        setArtifactsStatus('Generating artifacts...', 'pending');
        setActionBanner('Generating adoption artifacts from wizard configuration...', 'pending');
        logToolAction('generate', 'started');
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
            setActionBanner('Artifact generation failed (' + response.status + ').', 'error');
            logToolAction('generate', 'failed: ' + message);
            return false;
        }
        generated = await response.json();
        renderSnippet();
        setArtifactsStatus(
            'Artifacts generated. Use the tabs to view YAML, env vars, Java notes, copy manifest, adoption guide, library dependency, and PingOne Admin checklist. Click Copy to copy the active tab.',
            'success'
        );
        setActionBanner('Adoption artifacts generated successfully.', 'success');
        logToolAction('generate', 'completed');
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
        await persistWizardConfig();
        setDiagnosticsStatus('Running connectivity checks against the running application...', 'pending');
        setActionBanner('Running runtime diagnostics (issuer metadata and JWKS)...', 'pending');
        diagnosticsOutput.textContent = 'Running checks...';
        logToolAction('diagnostics', 'started');
        try {
            const response = await fetch('/tool/api/diagnostics');
            if (!response.ok) {
                throw new Error('Diagnostics request failed with status ' + response.status);
            }
            const data = await response.json();
            diagnosticsOutput.textContent = JSON.stringify(data, null, 2);
            const metadata = data.connectivityChecks?.metadata;
            const jwks = data.connectivityChecks?.jwks;
            const allPass = metadata?.status === 'PASS' && jwks?.status === 'PASS';
            const summary = allPass
                ? 'Connectivity checks passed. Metadata and JWKS endpoints are reachable.'
                : 'Some connectivity checks failed. Review the JSON output below.';
            setDiagnosticsStatus(summary, allPass ? 'success' : 'error');
            setActionBanner(summary, allPass ? 'success' : 'error');
            logToolAction('diagnostics', allPass ? 'all checks passed' : 'one or more checks failed');
            if (location.hash !== '#diagnostics') {
                location.hash = 'diagnostics';
            }
        } catch (error) {
            diagnosticsOutput.textContent = String(error);
            setDiagnosticsStatus('Diagnostics failed: ' + error.message, 'error');
            setActionBanner('Diagnostics failed: ' + error.message, 'error');
            logToolAction('diagnostics', 'failed: ' + error.message);
        }
    }

    async function loadRuntimeDiagnostics() {
        setActionBanner('Loading runtime defaults from application.yml...', 'pending');
        const response = await fetch('/tool/api/runtime-defaults');
        if (!response.ok) {
            setActionBanner('Failed to load runtime defaults.', 'error');
            logToolAction('load-runtime', 'failed');
            return;
        }
        const values = await response.json();
        applyFormValues(values);
        await persistWizardConfig();
        setArtifactsStatus(
            'Loaded application.yml runtime defaults into the wizard and saved to encrypted session.',
            'success'
        );
        setActionBanner('Runtime defaults loaded into the wizard and saved to session.', 'success');
        logToolAction('load-runtime', 'completed');
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
        document.getElementById('btn-tool-login')?.addEventListener('click', runToolOAuthLogin);
        document.getElementById('btn-tool-logout')?.addEventListener('click', runToolOAuthLogout);
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
            await loadSavedWizardConfig();
            await showOAuthReturnMessage();
            await refreshAuthStatus();
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
