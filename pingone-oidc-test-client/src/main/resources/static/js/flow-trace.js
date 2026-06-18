(function () {
    const diagramEl = document.getElementById('flow-trace-diagram');
    const logEl = document.getElementById('flow-trace-log');
    const statusEl = document.getElementById('flow-trace-status');
    const activeFlowEl = document.getElementById('flow-trace-active-flow');
    const clearBtn = document.getElementById('btn-flow-trace-clear');
    const refreshBtn = document.getElementById('btn-flow-trace-refresh');

    let lastEventCount = 0;
    let pollTimer = null;
    let mermaidReady = false;

    function csrfHeaders() {
        const token = document.querySelector('meta[name=_csrf]')?.content;
        const header = document.querySelector('meta[name=_csrf_header]')?.content || 'X-CSRF-TOKEN';
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers[header] = token;
        }
        return headers;
    }

    function setStatus(message, tone) {
        if (!statusEl) {
            return;
        }
        statusEl.textContent = message;
        statusEl.className = 'action-status' + (tone ? ' ' + tone : '');
    }

    function formatTime(iso) {
        if (!iso) {
            return '';
        }
        try {
            return new Date(iso).toLocaleTimeString();
        } catch (error) {
            return iso;
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function levelClass(level) {
        if (level === 'error') {
            return 'flow-event-error';
        }
        if (level === 'success') {
            return 'flow-event-success';
        }
        if (level === 'warn') {
            return 'flow-event-warn';
        }
        return 'flow-event-info';
    }

    function actorName(actor) {
        if (!actor) {
            return '';
        }
        if (typeof actor === 'object' && actor.displayName) {
            return actor.displayName;
        }
        const names = {
            BROWSER: 'Browser',
            TEST_CLIENT: 'Test Client',
            PINGONE: 'PingOne'
        };
        return names[actor] || String(actor);
    }

    function renderEventLog(events) {
        if (!logEl) {
            return;
        }
        if (!events?.length) {
            logEl.innerHTML = '<p class="flow-trace-empty">No flow events yet. Run login, logout, or connectivity checks to populate the trace.</p>';
            return;
        }
        logEl.innerHTML = events.map(event => {
            const http = event.httpMethod && event.path
                ? `<span class="flow-event-http">${escapeHtml(event.httpMethod)} ${escapeHtml(event.path)}${event.httpStatus != null ? ' → ' + event.httpStatus : ''}</span>`
                : '';
            return `<article class="flow-event ${levelClass(event.level)}" data-sequence="${event.sequence}">
                <header>
                    <span class="flow-event-seq">#${event.sequence}</span>
                    <span class="flow-event-time">${escapeHtml(formatTime(event.timestamp))}</span>
                    <span class="flow-event-actors">${escapeHtml(actorName(event.fromActor))} → ${escapeHtml(actorName(event.toActor))}</span>
                </header>
                <strong class="flow-event-label">${escapeHtml(event.label)}</strong>
                <p class="flow-event-message">${escapeHtml(event.message)}</p>
                ${http}
            </article>`;
        }).join('');
        const latest = logEl.querySelector(`[data-sequence="${events[events.length - 1].sequence}"]`);
        latest?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }

    async function ensureMermaid() {
        if (mermaidReady || typeof mermaid === 'undefined') {
            return mermaidReady;
        }
        mermaid.initialize({
            startOnLoad: false,
            theme: 'neutral',
            securityLevel: 'strict',
            sequence: { useMaxWidth: true, showSequenceNumbers: true }
        });
        mermaidReady = true;
        return true;
    }

    async function renderDiagram(mermaidSource) {
        if (!diagramEl) {
            return;
        }
        if (!mermaidSource?.trim()) {
            diagramEl.innerHTML = '<p class="flow-trace-empty">Sequence diagram will appear here.</p>';
            return;
        }
        const ready = await ensureMermaid();
        if (!ready) {
            diagramEl.innerHTML = '<pre class="json-block">' + escapeHtml(mermaidSource) + '</pre>';
            return;
        }
        const id = 'flow-trace-mermaid-' + Date.now();
        try {
            const { svg } = await mermaid.render(id, mermaidSource);
            diagramEl.innerHTML = svg;
        } catch (error) {
            console.warn('Mermaid render failed', error);
            diagramEl.innerHTML = '<pre class="json-block">' + escapeHtml(mermaidSource) + '</pre>';
        }
    }

    function renderTrace(view) {
        if (!view) {
            return;
        }
        if (activeFlowEl) {
            activeFlowEl.textContent = view.activeFlowName || 'general';
        }
        const events = view.events || [];
        if (events.length !== lastEventCount) {
            lastEventCount = events.length;
            setStatus(events.length
                ? `${events.length} event(s) in current session trace`
                : 'Waiting for login, logout, or API activity', events.length ? 'success' : '');
        }
        renderEventLog(events);
        renderDiagram(view.mermaidSequence);
    }

    async function fetchTrace() {
        const response = await fetch('/tool/api/flow/trace', { cache: 'no-store' });
        if (!response.ok) {
            throw new Error('Flow trace request failed with status ' + response.status);
        }
        return response.json();
    }

    async function refreshTrace() {
        try {
            const view = await fetchTrace();
            renderTrace(view);
            return view;
        } catch (error) {
            setStatus('Could not load flow trace: ' + error.message, 'error');
            console.warn(error);
            return null;
        }
    }

    async function clearTrace() {
        try {
            const response = await fetch('/tool/api/flow/clear', {
                method: 'POST',
                headers: csrfHeaders()
            });
            if (!response.ok) {
                throw new Error('Clear failed with status ' + response.status);
            }
            lastEventCount = 0;
            await refreshTrace();
            setStatus('Flow trace cleared', 'success');
        } catch (error) {
            setStatus('Could not clear flow trace: ' + error.message, 'error');
        }
    }

    async function recordClientEvent(flowName, label, message, level) {
        try {
            const response = await fetch('/tool/api/flow/client-event', {
                method: 'POST',
                headers: csrfHeaders(),
                body: JSON.stringify({ flowName, label, message, level: level || 'info' })
            });
            if (!response.ok) {
                return;
            }
            const view = await response.json();
            renderTrace(view);
        } catch (error) {
            console.warn('Failed to record client flow event', error);
        }
    }

    function startPolling() {
        stopPolling();
        pollTimer = window.setInterval(refreshTrace, 2000);
    }

    function stopPolling() {
        if (pollTimer != null) {
            window.clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function bindControls() {
        clearBtn?.addEventListener('click', clearTrace);
        refreshBtn?.addEventListener('click', refreshTrace);
    }

    bindControls();
    refreshTrace();
    startPolling();

    window.PingOneFlowTrace = {
        refresh: refreshTrace,
        clear: clearTrace,
        recordClientEvent,
        startPolling,
        stopPolling
    };
})();
