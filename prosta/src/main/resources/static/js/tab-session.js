(function () {
    'use strict';
    const script = document.currentScript;
    const context = new URL(script.src).pathname.replace(/\/js\/tab-session\.js$/, '');
    const storageKey = 'prosta.tab.' + context;
    const url = new URL(location.href);
    const bootstrap = script.dataset.bootstrap === 'true';
    const nativeFetch = window.fetch.bind(window);
    const channel = new BroadcastChannel(storageKey);
    const owner = randomId();
    let token = url.searchParams.get('_tab') || sessionStorage.getItem(storageKey) || randomId();
    let active = false;
    let collision = false;

    function randomId() {
        const bytes = crypto.getRandomValues(new Uint8Array(16));
        return Array.from(bytes, (b, i) =>
            ([4, 6, 8, 10].includes(i) ? '-' : '') + b.toString(16).padStart(2, '0')).join('');
    }

    channel.onmessage = function (event) {
        const message = event.data;
        if (message.token !== token || message.owner === owner) return;
        if (message.type === 'probe' && !collision) {
            channel.postMessage({ type: 'occupied', token: token, owner: owner });
        } else if (message.type === 'occupied') {
            collision = true;
        }
    };

    function localUrl(value) {
        const target = new URL(value, location.href);
        if (target.origin !== location.origin
                || !(target.pathname === context || target.pathname.startsWith(context + '/'))
                || /\/(css|js|images|fonts)\//.test(target.pathname)) return null;
        target.searchParams.delete('_nav');
        target.searchParams.set('_tab', token);
        return target;
    }

    // Duplicated tabs inherit sessionStorage. Probe live peers before allowing any request to use that identity.
    const ready = (async function () {
        channel.postMessage({ type: 'probe', token: token, owner: owner });
        await new Promise(resolve => setTimeout(resolve, 100));
        if (collision) token = randomId();
        sessionStorage.setItem(storageKey, token);
        active = true;
        if (bootstrap || collision) {
            const response = await nativeFetch(context + '/api/tab-navigation', {
                headers: { 'X-Prosta-Tab': token }, credentials: 'same-origin'
            });
            if (!response.ok) throw new Error('Tab navigation failed: ' + response.status);
            const result = await response.json();
            const target = localUrl(location.href);
            target.searchParams.set('_nav', result.ticket);
            location.replace(target.href);
            return false;
        }
        url.searchParams.delete('_nav');
        history.replaceState(history.state, '', url.href);
        return true;
    })();
    ready.catch(error => console.error('Tab session initialization failed', error));

    if (!bootstrap) {
        const originalFetch = window.fetch;
        // Only same-app requests carry the tab header; the database login token never leaves the server.
        window.fetch = function (input, options) {
            const value = input instanceof Request ? input.url : input;
            const target = localUrl(value);
            if (!target) return originalFetch.call(this, input, options);
            const request = new Request(input, options);
            const headers = new Headers(request.headers);
            headers.set('X-Prosta-Tab', token);
            return ready.then(ok => {
                if (!ok) throw new Error('Tab session changed; reload this page');
                return originalFetch.call(window, request, { headers: headers }).then(response => {
                    if (response.status === 401 && response.headers.get('X-Prosta-Login-Expired') === 'true') {
                        location.replace(localUrl(context + '/login/error').href);
                    }
                    return response;
                });
            });
        };
        document.addEventListener('click', function (event) {
            const link = event.target.closest('a[href]');
            if (!link || link.getAttribute('href').startsWith('#')) return;
            const target = localUrl(link.href);
            if (!target) return;
            if (event.ctrlKey || event.metaKey || event.shiftKey || link.target === '_blank') {
                target.searchParams.delete('_tab');
            }
            link.href = target.href;
        }, true);
        function prepareForm(form, submitter) {
            const target = localUrl(form.action);
            if (!target) return;
            // Keep one form field instead of a second query value: the server rejects ambiguous tab IDs.
            target.searchParams.delete('_tab');
            form.action = target.href;
            let field = form.querySelector('input[name="_tab"]');
            if (!field) {
                field = document.createElement('input');
                field.type = 'hidden';
                field.name = '_tab';
                form.append(field);
            }
            field.value = token;
            if (submitter && submitter.hasAttribute('formaction')) {
                const action = localUrl(submitter.formAction);
                if (action) {
                    action.searchParams.delete('_tab');
                    submitter.formAction = action.href;
                }
            }
        }
        document.addEventListener('submit', function (event) {
            if (!active || collision) {
                event.preventDefault();
                const form = event.target;
                const submitter = event.submitter;
                ready.then(ok => {
                    if (ok) form.requestSubmit(submitter);
                }).catch(error => console.error('Tab form submission failed', error));
                return;
            }
            prepareForm(event.target, event.submitter);
        }, true);
        const originalSubmit = HTMLFormElement.prototype.submit;
        // Timer-driven form.submit() bypasses the submit event, so it needs the same tab preparation.
        HTMLFormElement.prototype.submit = function () {
            if (!active || collision) {
                const form = this;
                ready.then(ok => {
                    if (ok) {
                        prepareForm(form);
                        originalSubmit.call(form);
                    }
                }).catch(error => console.error('Tab form submission failed', error));
                return;
            }
            prepareForm(this);
            return originalSubmit.call(this);
        };
    }
    window.addEventListener('pagehide', function () { active = false; channel.close(); });
    window.addEventListener('pageshow', function (event) {
        // A bfcache page can hold a logged-out identity or stale answers; revalidate on restoration.
        if (event.persisted) location.reload();
    });
})();
